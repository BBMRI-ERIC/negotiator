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
 * The two behaviours ADR 0005 deliberately changes, pinned as they behave <b>today</b>.
 *
 * <p><b>A red test in this class is the cutover succeeding, not a regression.</b> Every other class
 * in the characterization tree states what must survive the Lifecycle redesign byte for byte. This
 * one states the opposite: two live bugs that ADR 0005 exists to fix. Freezing them as parity would
 * have made the fix look like a break, so they are frozen here instead, each test carrying the
 * behaviour that should replace it. A later session that finds one of these red should read the
 * test's own comment, confirm the new behaviour is the one named there, and delete or invert the
 * test - not repair the production code.
 *
 * <p>The class is excluded from the parity gate mechanically, by the JUnit tag {@code
 * intended-delta} - see this package's {@code package-info}.
 *
 * <h2>Delta A - Possible Events include Events that cannot fire</h2>
 *
 * <p>{@code ResourceLifecycleServiceImpl.getPossibleEvents} computes its set from the Definition
 * graph and the caller's Required Authority alone. It never consults the Information Requirement
 * gate, which is the first thing {@code sendEvent} enforces. So an Event whose Requirement is unmet
 * is offered, advertised as a link on the Resource listing, and then refused when clicked. ADR 0005
 * makes one Evaluation Pipeline serve both the listing and the firing, and a blocked Event is
 * simply omitted.
 *
 * <h2>Delta B - the requirement hint links</h2>
 *
 * <p>{@code ResourceWithStatusAssembler} adds one link per Information Requirement row and one per
 * Information Submission row, each under its own rel carrying the row's numeric id ({@code
 * requirement-7}, {@code submission-3}). ADR 0005 changes three things about them: the inclusion
 * condition becomes structural reachability rather than "a lifecycle link for this Event was
 * already built", the display name becomes the Event's human label rather than the raw key, and the
 * per-row rels collapse into array-valued {@code requirement} and {@code submission} rels.
 *
 * <h2>The two known frontend breakages</h2>
 *
 * <p>Delta B's rel collapse breaks the frontend in two places, both in {@code
 * frontend/src/components/ResourceItem.vue}:
 *
 * <ul>
 *   <li>line 97, {@code getSubmissionLinks}: {@code Object.entries(resource._links).filter(([key])
 *       => key.startsWith('submission-'))};
 *   <li>line 103, {@code getRequirementLinks}: the same over {@code 'requirement-'}.
 * </ul>
 *
 * <p>An array-valued {@code submission} / {@code requirement} rel has no trailing hyphen, so both
 * filters match nothing and every requirement hint and submission link disappears from the
 * Negotiation page. Repairing the prefix is not enough either: both filters end {@code .map(([,
 * value]) => value)} and the results are read as single link objects ({@code link.href} at lines 59
 * and 66, {@code link.title} at line 67, {@code link.name} at line 60), which an array is not.
 * Standing decision 5 assigns those repairs to whichever slab breaks them - that is the slab that
 * lands ADR 0005, not this one.
 *
 * <p>Delta A does not break the frontend. {@code getLifecycleLinks} (line 108) filters on {@code
 * link.title === 'Next Lifecycle event'}, and offering fewer such links renders fewer buttons -
 * which is the point of the fix. That title string is nonetheless a magic constant the cutover must
 * keep, and it is recorded here so a rename is a decision rather than a surprise.
 *
 * <h2>Ordering</h2>
 *
 * <p>{@code @DirtiesContext} per method, for the reason ticket 05 recorded: the Requirement lookup
 * is global, so an {@code information_requirement} row left behind would block that Event for every
 * later test in the run.
 */
@IntegrationTest(loadTestData = true)
@AutoConfigureMockMvc
@Import(LifecycleTestAdapterConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Tag(IntendedDeltasAdr0005WillInvertTest.INTENDED_DELTA)
class IntendedDeltasAdr0005WillInvertTest {

  /**
   * The tag the parity gate excludes on. It is a compile-time constant so the annotation and the
   * documented Maven flags cannot drift apart.
   */
  static final String INTENDED_DELTA = "intended-delta";

  private static final String RESOURCES_ENDPOINT = "/v3/negotiations/%s/resources";

  /** Offered to an administrator from the subject Resource's seeded State. */
  private static final String GATED_EVENT = "CONTACT";

  /** The other Event offered from that State, so "one rel per row" has two rows to be about. */
  private static final String SECOND_OFFERED_EVENT = "MARK_AS_UNREACHABLE";

  /**
   * Carries a Transition in the graph, but not from the State the subject Resource is in - so no
   * lifecycle link for it is ever built here, whoever is asking.
   */
  private static final String EVENT_WITH_NO_TRANSITION_FROM_HERE = "GRANT_ACCESS_TO_RESOURCE";

  /**
   * The Information Requirement gate's refusal, verbatim. Owned by ticket 05's {@code
   * ResourceInformationRequirementGateTest}, which pins the whole refusal; it is repeated here only
   * so that "and then it is refused" names the gate rather than any refusal at all.
   */
  private static final String UNMET_REQUIREMENT_MESSAGE =
      "The requirement for this operation was not met."
          + " Please make sure you have submitted the required form and try again.";

  /** Title of every link the assembler builds for an Information Submission. */
  private static final String SUBMISSION_LINK_TITLE = "Submitted Information";

  /** Title the assembler stamps on a lifecycle link, which is how the frontend recognises one. */
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
   * <b>Intended delta, ADR 0005.</b> After the cutover the Information Requirement check is a
   * Built-in Stage of the one Evaluation Pipeline that answers both "what could fire now" and "may
   * this fire now", so a blocked Event is omitted from Possible Events. This assertion must then
   * fail: the set must <em>not</em> contain {@code CONTACT} while its Requirement is unmet, and the
   * second half of the test - the refusal - becomes unreachable through this route.
   *
   * <p>Both halves are in one method on purpose. Separately, "the Event is offered" and "the Event
   * is refused" are two ordinary facts; together they are the dead click ADR 0005 names as its
   * motivating bug.
   */
  @Test
  @DisplayName("delta A: an Event with an unmet Requirement is offered, then refused when fired")
  void deltaA_possibleEvents_offerAnEventTheRequirementGateThenRefuses() {
    authenticateAs(ADMIN);
    requireInformationFor(jdbcTemplate, GATED_EVENT);

    assertThat(adapter.possibleResourceEvents(NEGOTIATION, RESOURCE))
        .as(
            "today's Possible Events are computed from the graph and Required Authority alone,"
                + " so the Information Requirement gate never reaches them")
        .contains(GATED_EVENT);

    assertThatThrownBy(() -> adapter.sendResourceEvent(NEGOTIATION, RESOURCE, GATED_EVENT))
        .as("and the very same Event is refused by the gate the listing did not consult")
        .hasMessage(UNMET_REQUIREMENT_MESSAGE);
  }

  /**
   * <b>Intended delta, ADR 0005.</b> The same delta as seen by a client: the Resource listing
   * builds a lifecycle link per offered Event, so the unfireable Event is advertised in HAL too.
   * After the cutover the listing draws on the corrected Possible Events and this link must be
   * gone.
   *
   * <p>The link's {@code title} is asserted because it is the only thing the frontend uses to tell
   * a lifecycle link from any other rel.
   */
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
  // Delta B - rel naming
  // --------------------------------------------------------------------------------------------

  /**
   * <b>Intended delta, ADR 0005.</b> Today each Information Requirement row gets a rel of its own,
   * named {@code requirement-} plus that row's database id, and each is a single link object. After
   * the cutover there is one array-valued {@code requirement} rel holding both hints, so every
   * assertion below must fail: {@code requirement-<id>} must be absent and {@code requirement}
   * present as an array.
   */
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
        .as("there is no array-valued rel today; ADR 0005 introduces one")
        .isFalse();
  }

  /**
   * <b>Intended delta, ADR 0005.</b> The Submission half of the same rel shape, and the one place
   * both halves are visible at once: filing a Submission removes the Requirement hint and adds a
   * {@code submission-<id>} rel. After the cutover both become array-valued rels named {@code
   * requirement} and {@code submission}.
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
        .as("there is no array-valued rel today; ADR 0005 introduces one")
        .isFalse();
    assertThat(links.has("requirement-" + requirement))
        .as("a Requirement with a Submission on file is no longer hinted at")
        .isFalse();
  }

  // --------------------------------------------------------------------------------------------
  // Delta B - inclusion condition
  // --------------------------------------------------------------------------------------------

  /**
   * <b>Intended delta, ADR 0005.</b> Today a Requirement hint appears only if a lifecycle link for
   * its Event was already built - that is, only if this caller is being offered that Event right
   * now. So the condition is caller-dependent: the administrator above sees the hint, and the
   * Negotiation's creator, who is offered nothing from this State, sees the same Requirement row
   * not at all.
   *
   * <p>ADR 0005 replaces the condition with structural reachability - a Transition for this Event
   * exists from the current State - which is a property of the graph and not of the caller. {@code
   * SUBMITTED --CONTACT--> REPRESENTATIVE_CONTACTED} is such a Transition, so after the cutover the
   * creator must see the hint and this assertion must fail.
   *
   * <p>The ADR's own reason for the change is worth restating: once delta A lands, the blocked
   * Event is no longer offered, so "a lifecycle link was already built" would be false for
   * precisely the Event whose hint matters most.
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
   * The other half of today's inclusion condition, and the half ADR 0005 <b>keeps</b>: a
   * Requirement for an Event that has no Transition out of the current State is not hinted at.
   * Structural reachability excludes it too, so unlike its siblings this test is expected to keep
   * passing after the cutover. It is here because "the inclusion condition" is not pinned by its
   * positive case alone - without this, an assembler that hinted at every Requirement row would
   * satisfy the rest of this class.
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
  // Delta B - display name
  // --------------------------------------------------------------------------------------------

  /**
   * <b>Intended delta, ADR 0005.</b> A Requirement hint's display name is the raw Event key with
   * the word "requirement" appended - {@code "CONTACT requirement"} - while its title is the name
   * of the Access Form to be filled in. ADR 0005 makes the display name the Event's human label, so
   * after the cutover the name must become that label and this assertion must fail.
   *
   * <p>The label is read from the committed Event metadata rather than transcribed, so the test can
   * say "today's name is not the label" mechanically. The Access Form name is read from the row the
   * fixture attached the Requirement to, so what is pinned is the rule - title is the Form's name -
   * rather than one seeded string.
   */
  @Test
  @WithMockNegotiatorUser(id = ADMIN, authorities = "ROLE_ADMIN")
  @DisplayName("delta B: a Requirement hint's display name is the raw Event key, not its label")
  void deltaB_requirementLink_displayNameIsTheRawEventKey() throws Exception {
    long requirement = requireInformationFor(jdbcTemplate, GATED_EVENT);

    JsonNode link = linksOfTheOnlyResource().get("requirement-" + requirement);

    assertThat(link.get("name").asText()).isEqualTo(GATED_EVENT + " requirement");
    assertThat(link.get("name").asText())
        .as("ADR 0005 replaces this with the Event's published label")
        .isNotEqualTo(publishedLabelOf(GATED_EVENT));
    assertThat(link.get("title").asText())
        .as("the title carries the Access Form the caller has to fill in")
        .isEqualTo(accessFormNameOf(requirement));
  }

  /**
   * The Submission link's two texts, which the ADR does not mention and which therefore have to be
   * written down before anyone rewrites the assembler around them: a fixed title, and a display
   * name that is the Access Form's name rather than anything about the Event. The frontend renders
   * the name (line 60 of {@code ResourceItem.vue}), so it is user-facing.
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

  /**
   * The {@code _links} object of the Negotiation's single Resource, straight out of the response
   * body. {@code negotiation-1} has exactly one Resource, which is what makes indexing into the
   * collection safe; it is asserted rather than assumed.
   */
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

  /** The name of the Access Form an Information Requirement row demands. */
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
