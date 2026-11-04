package eu.bbmri_eric.negotiator.characterization.delta;

import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.ADMIN;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.CREATOR;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.NEGOTIATION;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.RESOURCE;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.authenticateAs;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.requireInformationFor;
import static eu.bbmri_eric.negotiator.characterization.service.SeededResourceSubject.submitInformationFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapter;
import eu.bbmri_eric.negotiator.characterization.adapter.LifecycleTestAdapterConfig;
import eu.bbmri_eric.negotiator.characterization.rest.CanonicalJson;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Two behaviours of the Resource listing that are meant to change, pinned as they behave today.
 *
 * <p>A red test in this class means the intended change landed, not a regression: confirm the new
 * behaviour matches the one the test names, then delete or invert the test rather than repair the
 * production code. The JUnit tag {@code intended-delta} keeps the class out of the parity run.
 *
 * <p>Delta A: {@code ResourceLifecycleServiceImpl.getPossibleEvents} reads only the graph and the
 * caller's Required Authority, never the Information Requirement gate that {@code sendEvent}
 * enforces. An Event with an unmet Requirement is offered, shown as a link, and refused when fired.
 *
 * <p>Delta B: {@code ResourceWithStatusAssembler} adds one link per Information Requirement row and
 * one per Information Submission row, each under a rel carrying the row id ({@code requirement-7},
 * {@code submission-3}). The intended change includes a Requirement hint whenever a Transition for
 * its Event leaves the current State, names it after the Event's label, and collapses the per-row
 * rels into array-valued {@code requirement} and {@code submission} rels.
 *
 * <p>The rel collapse breaks {@code getSubmissionLinks} and {@code getRequirementLinks} in {@code
 * frontend/src/components/ResourceItem.vue}. Both filter on the {@code submission-} and {@code
 * requirement-} prefixes and read each match as a single link object, so they need fixing in the
 * same change. Delta A does not break the frontend, which picks lifecycle links by the title {@code
 * Next Lifecycle event}; that title has to stay.
 *
 * <p>{@code @DirtiesContext} runs per method because the Requirement lookup is global: a leftover
 * {@code information_requirement} row would block that Event for every later test in the run.
 */
@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag(RequirementHintLinksIntendedChangeTest.INTENDED_DELTA)
class RequirementHintLinksIntendedChangeTest {

  /** The tag the parity run excludes. */
  static final String INTENDED_DELTA = "intended-delta";

  private static final String RESOURCES_ENDPOINT = "/v3/negotiations/%s/resources";

  private static final String GATED_EVENT = "CONTACT";

  /** A second offered Event, so "one rel per row" has two rows to check. */
  private static final String SECOND_OFFERED_EVENT = "MARK_AS_UNREACHABLE";

  /** Has a Transition in the graph, but none from the subject Resource's State. */
  private static final String EVENT_WITH_NO_TRANSITION_FROM_HERE = "GRANT_ACCESS_TO_RESOURCE";

  /**
   * The Information Requirement gate's refusal, so the test proves this gate refused the Event and
   * not some other check. {@code ResourceInformationRequirementGateTest} pins the full refusal.
   */
  private static final String UNMET_REQUIREMENT_MESSAGE =
      "Contact cannot fire for this Resource until its Information Requirement form is submitted.";

  private static final String SUBMISSION_LINK_TITLE = "Submitted Information";

  /** The frontend tells a lifecycle link from other rels by this title alone. */
  private static final String LIFECYCLE_LINK_TITLE = "Next Lifecycle event";

  private static final String PUBLISHED_EVENTS = "characterization/rest/resource-events.json";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired LifecycleTestAdapter adapter;
  @Autowired JdbcTemplate jdbcTemplate;
  @Autowired MockMvc mockMvc;

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  // --------------------------------------------------------------------------------------------
  // Delta A
  // --------------------------------------------------------------------------------------------

  /**
   * Intended to change: Possible Events should leave out an Event its Requirement blocks. Both
   * halves sit in one method because together they make the dead click: offered, then refused.
   */
  @Test
  @DisplayName("delta A: an Event with an unmet Requirement is offered, then refused when fired")
  void deltaA_possibleEvents_offerAnEventTheRequirementGateThenRefuses() {
    authenticateAs(ADMIN);
    requireInformationFor(jdbcTemplate, GATED_EVENT);

    assertThat(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE))
        .as(
            "Possible Events read only the graph and Required Authority,"
                + " so the Information Requirement gate never reaches them")
        .contains(GATED_EVENT);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT))
        .as("and the very same Event is refused by the gate the listing did not consult")
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
  }

  /** Intended to change: the listing should not show a link for an Event that cannot fire. */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName("delta A: the Resource listing advertises the same unfireable Event as a HAL link")
  void deltaA_theResourceListing_advertisesTheUnfireableEvent() throws Exception {
    requireInformationFor(jdbcTemplate, GATED_EVENT);

    JsonNode links = linksOfTheOnlyResource();

    assertThat(links.has(GATED_EVENT))
        .as("the assembler's lifecycle links come from the same uncorrected set")
        .isTrue();
    assertThat(links.get(GATED_EVENT).get("title").asText()).isEqualTo(LIFECYCLE_LINK_TITLE);
  }

  // --------------------------------------------------------------------------------------------
  // Delta B: rel naming
  // --------------------------------------------------------------------------------------------

  /** Intended to change: one array-valued {@code requirement} rel should hold both hints. */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName("delta B: Requirement links use one rel per row, named after the row's numeric id")
  void deltaB_requirementLinks_useOneRelPerRowCarryingTheRowId() throws Exception {
    long first = requireInformationFor(jdbcTemplate, GATED_EVENT);
    long second = requireInformationFor(jdbcTemplate, SECOND_OFFERED_EVENT);

    JsonNode links = linksOfTheOnlyResource();

    assertThat(links.has("requirement-" + first)).isTrue();
    assertThat(links.has("requirement-" + second)).isTrue();
    assertThat(links.get("requirement-" + first).isObject())
        .as("one link object per rel, not HAL's array form for multiples")
        .isTrue();
    assertThat(links.get("requirement-" + first).get("href").asText())
        .as("the rel's numeric suffix is the same row id the href addresses")
        .endsWith("/" + first);
    assertThat(links.has("requirement"))
        .as("no array-valued rel exists yet")
        .isFalse();
  }

  /**
   * Intended to change: Submissions should share one array-valued {@code submission} rel. Filing a
   * Submission also removes the Requirement hint.
   */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName("delta B: Submission links use one rel per row, named after the row's numeric id")
  void deltaB_submissionLinks_useOneRelPerRowCarryingTheRowId() throws Exception {
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);
    long submission = submitInformationFor(jdbcTemplate, requirement);

    JsonNode links = linksOfTheOnlyResource();

    assertThat(links.has("submission-" + submission)).isTrue();
    assertThat(links.get("submission-" + submission).isObject()).isTrue();
    assertThat(links.get("submission-" + submission).get("href").asText())
        .endsWith("/" + submission);
    assertThat(links.has("submission"))
        .as("no array-valued rel exists yet")
        .isFalse();
    assertThat(links.has("requirement-" + requirement))
        .as("a Requirement with a Submission on file is not hinted at")
        .isFalse();
  }

  // --------------------------------------------------------------------------------------------
  // Delta B: inclusion condition
  // --------------------------------------------------------------------------------------------

  /**
   * Intended to change. A Requirement hint appears only if the assembler already built a lifecycle
   * link for its Event, so it depends on the caller: the creator, offered nothing from this State,
   * sees no hint. The hint should depend on the graph instead, and {@code SUBMITTED --CONTACT-->
   * REPRESENTATIVE_CONTACTED} leaves this State, so the creator should see it. Once delta A is fixed
   * the blocked Event gets no lifecycle link, so the current rule would hide exactly the hint that
   * matters.
   */
  @Test
  @WithMockNegotiatorUser(id = CREATOR)
  @DisplayName("delta B: a Requirement is hinted at only for a caller currently offered its Event")
  void deltaB_requirementLink_isOmittedForACallerNotOfferedTheEvent() throws Exception {
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);

    JsonNode links = linksOfTheOnlyResource();

    assertThat(links.has(GATED_EVENT))
        .as("the Negotiation's creator has no Required Authority for this Event from this State")
        .isFalse();
    assertThat(links.has("requirement-" + requirement))
        .as("so no lifecycle link exists for the inclusion condition to find")
        .isFalse();
  }

  /**
   * Not intended to change: a Requirement for an Event with no Transition out of the current State
   * gets no hint, under the current rule and the intended one. Without this case, an assembler that
   * hinted at every Requirement row would pass the rest of this class.
   */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName("delta B: a Requirement for an Event unreachable from this State is not hinted at")
  void deltaB_requirementLink_isOmittedWhenNoTransitionLeavesTheCurrentState() throws Exception {
    long reachable = requireInformationFor(jdbcTemplate, GATED_EVENT);
    long unreachable = requireInformationFor(jdbcTemplate, EVENT_WITH_NO_TRANSITION_FROM_HERE);

    JsonNode links = linksOfTheOnlyResource();

    assertThat(links.has("requirement-" + reachable))
        .as("the control: an offered Event is hinted at in the same response")
        .isTrue();
    assertThat(links.has("requirement-" + unreachable)).isFalse();
  }

  // --------------------------------------------------------------------------------------------
  // Delta B: display name
  // --------------------------------------------------------------------------------------------

  /**
   * Intended to change: the display name {@code "CONTACT requirement"} should become the Event's
   * label. The test reads the label from the committed Event metadata and the Access Form name from
   * the database, so it pins the rule and not one seeded string.
   */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName("delta B: a Requirement hint's display name is the raw Event key, not its label")
  void deltaB_requirementLink_displayNameIsTheRawEventKey() throws Exception {
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);

    JsonNode link = linksOfTheOnlyResource().get("requirement-" + requirement);

    assertThat(link.get("name").asText()).isEqualTo(GATED_EVENT + " requirement");
    assertThat(link.get("name").asText())
        .as("the intended name is the Event's published label")
        .isNotEqualTo(publishedLabelOf(GATED_EVENT));
    assertThat(link.get("title").asText())
        .as("the title carries the Access Form the caller has to fill in")
        .isEqualTo(accessFormNameOf(requirement));
  }

  /**
   * Not intended to change: a Submission link has a fixed title and is named after the Access Form.
   * {@code ResourceItem.vue} shows the name to users.
   */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName(
      "delta B: a Submission link is titled 'Submitted Information' and named after the "
          + "Access Form")
  void deltaB_submissionLink_isTitledFixedAndNamedAfterTheAccessForm() throws Exception {
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);
    long submission = submitInformationFor(jdbcTemplate, requirement);

    JsonNode link = linksOfTheOnlyResource().get("submission-" + submission);

    assertThat(link.get("title").asText()).isEqualTo(SUBMISSION_LINK_TITLE);
    assertThat(link.get("name").asText()).isEqualTo(accessFormNameOf(requirement));
  }

  // --------------------------------------------------------------------------------------------

  /** The {@code _links} of the Negotiation's only Resource. */
  private JsonNode linksOfTheOnlyResource() throws Exception {
    String body =
        mockMvc
            .perform(MockMvcRequestBuilders.get(RESOURCES_ENDPOINT.formatted(NEGOTIATION)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode resources = MAPPER.readTree(body).get("_embedded").get("resources");
    assertThat(resources).hasSize(1);
    assertThat(resources.get(0).get("sourceId").asText()).isEqualTo(RESOURCE);
    return resources.get(0).get("_links");
  }

  /** The label the metadata endpoint publishes for an Event, read from the committed artifact. */
  private static String publishedLabelOf(String event) {
    return CanonicalJson.publishedLabels(PUBLISHED_EVENTS, "events").get(event);
  }

  private String accessFormNameOf(long requirementId) {
    List<String> names =
        jdbcTemplate.queryForList(
            "select f.name from information_requirement r"
                + " join access_form f on f.id = r.required_access_form_id"
                + " where r.id = ?",
            String.class,
            requirementId);
    assertThat(names).hasSize(1);
    return names.get(0);
  }
}
