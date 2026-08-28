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
 * Persistence behaviour of the Definition Version Pin, the Definition Version that a Negotiation,
 * and separately each of its Resources, is pinned to when its Lifecycle starts.
 *
 * <p>The pin is stored with the work instead of resolved on demand, so publishing a new Definition
 * Version leaves work in progress on the graph it started under.
 *
 * <p>The class sits in the definition package because it needs the package-private {@link
 * LifecycleDefinitionRepository} to create a row to point at. It loads the test seed because
 * several tests cover rows that predate the column.
 *
 * <p>Both pins point at the one {@code NEGOTIATION}-scoped definition the fixtures build. The
 * schema does not check a pin against a Definition Version's {@code scope}, because Definition
 * Resolution matches the two, so a second family would add fixtures without testing a constraint.
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
   * A Negotiation with no pin is a valid row, because the column sits on a table with existing rows
   * that have no pin.
   */
  @Test
  void save_negotiationWithoutAPin_isAccepted() {
    Negotiation saved = negotiations.saveAndFlush(negotiationPinnedTo(null));
    entityManager.clear();

    assertNull(negotiations.findById(saved.getId()).orElseThrow().getLifecycleDefinitionId());
  }

  /**
   * The Resource pin is per link, so the test writes and reads it through the link. It persists
   * through {@link EntityManager} because a link has no repository. {@link Negotiation} owns the
   * collection, and its only public entry point, {@code addResource}, creates an unpinned link.
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
   * The link that {@code addResource} creates has no pin, because the Resource's Lifecycle has not
   * started yet.
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
   * Rows written before the column existed load with a null pin. The seed runs after the migration
   * and names neither column, so this also asserts that neither column has a default.
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
   * The pin is a foreign key, so a Negotiation cannot name a Definition Version that does not
   * exist. The test writes through {@link JdbcTemplate} because the pin has no setter and because
   * migrations write it with SQL that bypasses the mapping.
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

  /** The same foreign key on the link table. */
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
   * {@code ON DELETE RESTRICT} stops the deletion of a Definition Version that work is pinned to. A
   * cascade would delete the Negotiation along with the configuration it was submitted under.
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
