package eu.bbmri_eric.negotiator.lifecycle.definition;

import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.STANDARD_FAMILY;
import static eu.bbmri_eric.negotiator.lifecycle.definition.DefinitionFixtures.definitionIn;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.config.MockUserDetailsService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceLink;
import eu.bbmri_eric.negotiator.negotiation.NegotiationResourceLinkId;
import eu.bbmri_eric.negotiator.util.RepositoryTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Persistence behaviour of the Definition Version Pin — the Lifecycle Definition a Negotiation, and
 * separately each of its Resources, is pinned to when its Lifecycle starts.
 *
 * <p>The pin lives beside the work rather than being looked up on demand, so that publishing a new
 * Definition Version leaves work already in flight on the graph it started under.
 *
 * <p>The class sits in the definition package, not under {@code integration/repository/}, because
 * it needs {@link LifecycleDefinitionRepository} to have a definition row to point at, and that
 * repository is package private. It loads the test seed because half of what this slice has to
 * prove is about rows that <em>predate</em> the column.
 *
 * <p>Both pins point at the one {@code NEGOTIATION}-scoped definition the fixtures build, rather
 * than at a {@code NEGOTIATION} one and a {@code RESOURCE} one. Nothing constrains a pin against a
 * Definition Version's {@code scope} — matching the two is Definition Resolution's job, not this
 * column's — so a second family here would add fixture surface to every test below and document
 * nothing the schema enforces.
 */
@RepositoryTest(loadTestData = true)
@Import(MockUserDetailsService.class)
class DefinitionVersionPinRepositoryTest {

  private static final String PAYLOAD =
      "{\"project\":{\"title\":\"Title\",\"description\":\"Description\"}}";

  @Autowired LifecycleDefinitionRepository definitions;
  @Autowired NegotiationRepository negotiations;
  @Autowired EntityManager entityManager;
  @Autowired JdbcTemplate jdbcTemplate;

  private LifecycleDefinition definition;
  private DiscoveryService discoveryService;
  private Resource resource;

  @BeforeEach
  void setUp() {
    definition = definitions.saveAndFlush(definitionIn(STANDARD_FAMILY));
    Negotiation seeded = negotiations.findById("negotiation-1").orElseThrow();
    discoveryService = seeded.getDiscoveryService();
    resource = seeded.getResources().iterator().next();
  }

  @Test
  void save_negotiationPinnedToADefinition_roundTrips() {
    Negotiation saved = negotiations.saveAndFlush(negotiationPinnedTo(definition.getId()));
    entityManager.clear();

    Negotiation found = negotiations.findById(saved.getId()).orElseThrow();
    assertEquals(definition.getId(), found.getLifecycleDefinitionId());
  }

  /**
   * A Negotiation with no pin is a valid row. It has to be: the column is added to a table that
   * already has rows, and the data cutover that fills them in is a much later migration.
   */
  @Test
  void save_negotiationWithoutAPin_isAccepted() {
    Negotiation saved = negotiations.saveAndFlush(negotiationPinnedTo(null));
    entityManager.clear();

    assertNull(negotiations.findById(saved.getId()).orElseThrow().getLifecycleDefinitionId());
  }

  /**
   * The Resource pin is per link, so it is written and read through the link rather than through
   * its Negotiation. Persisted through {@link EntityManager} because a link has no repository of
   * its own — {@link Negotiation} owns the collection and its only public entry point, {@code
   * addResource}, deliberately creates an unpinned link.
   */
  @Test
  void save_resourceLinkPinnedToADefinition_roundTrips() {
    Negotiation negotiation = negotiations.saveAndFlush(negotiationPinnedTo(null));
    entityManager.persist(
        new NegotiationResourceLink(negotiation, resource, "SUBMITTED", definition.getId()));
    entityManager.flush();
    entityManager.clear();

    NegotiationResourceLink found =
        entityManager.find(
            NegotiationResourceLink.class, new NegotiationResourceLinkId(negotiation, resource));
    assertEquals(definition.getId(), found.getLifecycleDefinitionId());
  }

  /**
   * The link a Negotiation creates when a Resource is added to it carries no pin, because the
   * Resource's Lifecycle has not started yet. That is the shape {@code addResource} produces today.
   */
  @Test
  void save_resourceLinkWithoutAPin_isAccepted() {
    Negotiation negotiation = negotiations.saveAndFlush(negotiationPinnedTo(null));
    entityManager.persist(new NegotiationResourceLink(negotiation, resource, "SUBMITTED"));
    entityManager.flush();
    entityManager.clear();

    NegotiationResourceLink found =
        entityManager.find(
            NegotiationResourceLink.class, new NegotiationResourceLinkId(negotiation, resource));
    assertNull(found.getLifecycleDefinitionId());
  }

  /**
   * Rows written before the column existed still load, with a null pin. The seed runs after the
   * migration and names neither column, so this is also the assertion that neither carries a
   * default: a default would have filled these in.
   */
  @Test
  void load_seededRowsThatPredateTheColumn_haveANullPin() {
    Negotiation seeded = negotiations.findById("negotiation-1").orElseThrow();
    assertNull(seeded.getLifecycleDefinitionId());
    assertNull(
        jdbcTemplate.queryForObject(
            "SELECT lifecycle_definition_id FROM negotiation_resource_link"
                + " WHERE negotiation_id = 'negotiation-1'",
            Long.class));
  }

  /**
   * The pin is a foreign key, not a loose number: a Negotiation cannot claim a Definition Version
   * that does not exist. Written through {@link JdbcTemplate} because the pin has no setter, and
   * because the cutover that backfills it is SQL that bypasses the mapping entirely.
   */
  @Test
  void update_negotiationPinnedToAnUnknownDefinition_isRefusedByTheDatabase() {
    DataIntegrityViolationException refused =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                jdbcTemplate.update(
                    "UPDATE negotiation SET lifecycle_definition_id = -1"
                        + " WHERE id = 'negotiation-1'"));
    assertTrue(refused.getMessage().contains("fk_negotiation_lifecycle_definition"));
  }

  /** The same foreign key on the link table, which the cutover backfills separately. */
  @Test
  void update_resourceLinkPinnedToAnUnknownDefinition_isRefusedByTheDatabase() {
    DataIntegrityViolationException refused =
        assertThrows(
            DataIntegrityViolationException.class,
            () ->
                jdbcTemplate.update(
                    "UPDATE negotiation_resource_link SET lifecycle_definition_id = -1"
                        + " WHERE negotiation_id = 'negotiation-1'"));
    assertTrue(refused.getMessage().contains("fk_negotiation_resource_link_lifecycle_definition"));
  }

  /**
   * A Definition Version that work is pinned to cannot be deleted. The refusal is what {@code ON
   * DELETE RESTRICT} is for here: a cascade would delete the Negotiation along with the
   * configuration it was submitted under.
   */
  @Test
  void delete_aDefinitionPinnedByANegotiation_isRefused() {
    negotiations.saveAndFlush(negotiationPinnedTo(definition.getId()));

    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          definitions.delete(definition);
          definitions.flush();
        });
  }

  /** The same, through a Resource's pin. */
  @Test
  void delete_aDefinitionPinnedByAResourceLink_isRefused() {
    Negotiation negotiation = negotiations.saveAndFlush(negotiationPinnedTo(null));
    entityManager.persist(
        new NegotiationResourceLink(negotiation, resource, "SUBMITTED", definition.getId()));
    entityManager.flush();

    assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          definitions.delete(definition);
          definitions.flush();
        });
  }

  private Negotiation negotiationPinnedTo(Long definitionId) {
    return Negotiation.builder()
        .currentState("SUBMITTED")
        .discoveryService(discoveryService)
        .humanReadable("#1 Material Type: DNA")
        .payload(PAYLOAD)
        .lifecycleDefinitionId(definitionId)
        .build();
  }
}
