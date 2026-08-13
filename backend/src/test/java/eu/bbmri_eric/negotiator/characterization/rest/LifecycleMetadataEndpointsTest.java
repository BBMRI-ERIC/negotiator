package eu.bbmri_eric.negotiator.characterization.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Characterization of the four lifecycle metadata endpoints as they behave today.
 *
 * <p>These endpoints publish States and Events as a <em>fixed global universe</em>: each collection
 * is the full set of constants of one enum, whether or not the constant sits on any transition of
 * the Lifecycle. That property is the thing the cutover changes, so it is pinned here in full —
 * every member, its value, label, description, and the HAL links the assemblers add.
 *
 * <p>Assertions run against the response JSON rather than against mapped objects, because the point
 * is that the bytes on the wire survive the cutover. State and Event names appear only as string
 * literals so that this class still compiles once the enums are deleted.
 */
@IntegrationTest(loadTestData = true)
public class LifecycleMetadataEndpointsTest {

  private static final String NEGOTIATION_STATES = "/v3/negotiation-lifecycle/states";
  private static final String NEGOTIATION_EVENTS = "/v3/negotiation-lifecycle/events";
  private static final String RESOURCE_STATES = "/v3/resource-lifecycle/states";
  private static final String RESOURCE_EVENTS = "/v3/resource-lifecycle/events";

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private MvcResult get(String url) throws Exception {
    return mockMvc.perform(MockMvcRequestBuilders.get(url)).andReturn();
  }

  private String okHalBody(String url) throws Exception {
    return mockMvc
        .perform(MockMvcRequestBuilders.get(url))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private void assertCollectionMatchesFixture(String url, String rel, String fixture)
      throws Exception {
    assertEquals(
        CanonicalJson.fixture(fixture),
        CanonicalJson.canonicalizeCollection(okHalBody(url), rel).strip());
  }

  private void assertItemMatches(String url, String expectedJson) throws Exception {
    assertEquals(
        CanonicalJson.canonicalize(expectedJson).strip(),
        CanonicalJson.canonicalize(okHalBody(url)).strip());
  }

  // ---------------------------------------------------------------- collections

  @Test
  @DisplayName("Negotiation states collection: every member, value, label, description and link")
  void negotiationStatesCollection_isPinnedInFull() throws Exception {
    assertCollectionMatchesFixture(NEGOTIATION_STATES, "states", "negotiation-states.json");
  }

  @Test
  @DisplayName("Negotiation events collection: every member, value, label, description and link")
  void negotiationEventsCollection_isPinnedInFull() throws Exception {
    assertCollectionMatchesFixture(NEGOTIATION_EVENTS, "events", "negotiation-events.json");
  }

  @Test
  @DisplayName("Resource states collection: every member, value, ordinal, label, description, link")
  void resourceStatesCollection_isPinnedInFull() throws Exception {
    assertCollectionMatchesFixture(RESOURCE_STATES, "states", "resource-states.json");
  }

  @Test
  @DisplayName("Resource events collection: every member, value, label, description and link")
  void resourceEventsCollection_isPinnedInFull() throws Exception {
    assertCollectionMatchesFixture(RESOURCE_EVENTS, "events", "resource-events.json");
  }

  @Test
  @DisplayName("Collections publish exactly the enum universe, no more and no less")
  void collections_publishTheWholeEnumUniverse() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATION_STATES))
        .andExpect(jsonPath("$._embedded.states.length()").value(8));
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATION_EVENTS))
        .andExpect(jsonPath("$._embedded.events.length()").value(8));
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_STATES))
        .andExpect(jsonPath("$._embedded.states.length()").value(12));
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_EVENTS))
        .andExpect(jsonPath("$._embedded.events.length()").value(13));
  }

  @Test
  @DisplayName("Members that sit on no transition are published anyway")
  void collections_publishMembersThatSitOnNoTransition() throws Exception {
    // The universe is the enum, not the graph: RETURNED_FOR_RESUBMISSION has no transitions, and
    // RETURN_FOR_RESUBMISSION and OVERRIDE are attached to none either, yet all three are listed.
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_STATES))
        .andExpect(
            jsonPath("$._embedded.states[?(@.value == 'RETURNED_FOR_RESUBMISSION')]").isNotEmpty());
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_EVENTS))
        .andExpect(
            jsonPath("$._embedded.events[?(@.value == 'RETURN_FOR_RESUBMISSION')]").isNotEmpty())
        .andExpect(jsonPath("$._embedded.events[?(@.value == 'OVERRIDE')]").isNotEmpty());
  }

  // ---------------------------------------------------------------- single items

  @Test
  @DisplayName("Single Negotiation state")
  void singleNegotiationState_isPinnedInFull() throws Exception {
    assertItemMatches(
        NEGOTIATION_STATES + "/SUBMITTED",
        """
        {
          "value": "SUBMITTED",
          "label": "Under review",
          "description": "The negotiation has been submitted for review",
          "_links": {
            "states": {"href": "http://localhost/v3/negotiation-lifecycle/states"},
            "self": {"href": "http://localhost/v3/negotiation-lifecycle/states/SUBMITTED"}
          }
        }
        """);
  }

  @Test
  @DisplayName("Single Negotiation event")
  void singleNegotiationEvent_isPinnedInFull() throws Exception {
    assertItemMatches(
        NEGOTIATION_EVENTS + "/APPROVE",
        """
        {
          "value": "APPROVE",
          "label": "Approve",
          "description": "Approve the negotiation",
          "_links": {
            "events": {"href": "http://localhost/v3/negotiation-lifecycle/events"},
            "self": {"href": "http://localhost/v3/negotiation-lifecycle/events/APPROVE"}
          }
        }
        """);
  }

  @Test
  @DisplayName("Single Resource state, including the ordinal field only this DTO carries")
  void singleResourceState_isPinnedInFull() throws Exception {
    assertItemMatches(
        RESOURCE_STATES + "/RESOURCE_AVAILABLE",
        """
        {
          "value": "RESOURCE_AVAILABLE",
          "ordinal": 7,
          "label": "Resource Available",
          "description": "The resource is available for access",
          "_links": {
            "states": {"href": "http://localhost/v3/resource-lifecycle/states"},
            "self": {"href": "http://localhost/v3/resource-lifecycle/states/RESOURCE_AVAILABLE"}
          }
        }
        """);
  }

  @Test
  @DisplayName("Single Resource event")
  void singleResourceEvent_isPinnedInFull() throws Exception {
    assertItemMatches(
        RESOURCE_EVENTS + "/CONTACT",
        """
        {
          "value": "CONTACT",
          "label": "Contact",
          "description": "Contact representatives",
          "_links": {
            "events": {"href": "http://localhost/v3/resource-lifecycle/events"},
            "self": {"href": "http://localhost/v3/resource-lifecycle/events/CONTACT"}
          }
        }
        """);
  }

  // ---------------------------------------------------------------- HAL link structure

  @Test
  @DisplayName("HAL links on a collection: no root links, two rels per member")
  void halLinkStructure_ofACollection_isPinned() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_STATES))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").doesNotExist())
        .andExpect(jsonPath("$._embedded.states[*]._links.self.href").isArray())
        .andExpect(
            jsonPath("$._embedded.states[?(@.value == 'SUBMITTED')]._links.self.href")
                .value("http://localhost/v3/resource-lifecycle/states/SUBMITTED"))
        .andExpect(
            jsonPath("$._embedded.states[?(@.value == 'SUBMITTED')]._links.states.href")
                .value("http://localhost/v3/resource-lifecycle/states"))
        .andExpect(
            jsonPath("$._embedded.states[?(@.value == 'SUBMITTED')]._links.length()").value(2));
  }

  @Test
  @DisplayName("HAL links on a single item: the collection rel plus self, and nothing else")
  void halLinkStructure_ofASingleItem_isPinned() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_EVENTS + "/STEP_AWAY"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links.length()").value(2))
        .andExpect(
            jsonPath("$._links.events.href").value("http://localhost/v3/resource-lifecycle/events"))
        .andExpect(
            jsonPath("$._links.self.href")
                .value("http://localhost/v3/resource-lifecycle/events/STEP_AWAY"));
  }

  // ---------------------------------------------------------------- access

  @Test
  @DisplayName("The endpoints are anonymous: an unauthenticated client gets the payload")
  void endpoints_areReachableWithoutAuthentication() throws Exception {
    for (String url :
        new String[] {NEGOTIATION_STATES, NEGOTIATION_EVENTS, RESOURCE_STATES, RESOURCE_EVENTS}) {
      mockMvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isOk());
    }
  }

  @Test
  @WithUserDetails("researcher")
  @DisplayName("An authenticated client gets the identical payload")
  void endpoints_returnTheSamePayloadWhenAuthenticated() throws Exception {
    assertCollectionMatchesFixture(RESOURCE_STATES, "states", "resource-states.json");
    assertCollectionMatchesFixture(RESOURCE_EVENTS, "events", "resource-events.json");
  }

  // ---------------------------------------------------------------- failure mode

  /**
   * The state path variables bind straight to a Java enum, so an unrecognised name fails during
   * binding rather than in the handler, and {@code Enum.valueOf}'s message is surfaced verbatim as
   * the problem detail.
   *
   * <p>That message names the enum's fully-qualified class — a class the redesign deletes. So the
   * detail text is the one part of this response that <em>cannot</em> survive the cutover
   * unchanged, and pinning it verbatim would be pinning a delta as if it were parity. What is
   * pinned here is everything that can hold: the status, the content type, the problem shape, and
   * the fact that the detail is derived from the rejected name rather than from a fixed message.
   *
   * <p>The exact text today is recorded as a finding in the ticket, not asserted here. Recovering
   * it is a one-line change to this test if a later slab wants the before-picture.
   */
  private void assertUnrecognisedStateIsRejected(String collectionPath) throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(collectionPath + "/NOT_A_STATE"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.title").value("Bad request."))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.detail").value(startsWith("No enum constant ")))
        .andExpect(jsonPath("$.detail").value(endsWith(".NOT_A_STATE")));
  }

  @Test
  @DisplayName("Unrecognised Negotiation state: 400 with a detail derived from the rejected name")
  void unrecognisedNegotiationState_isPinned() throws Exception {
    assertUnrecognisedStateIsRejected(NEGOTIATION_STATES);
  }

  @Test
  @DisplayName("Unrecognised Resource state: 400 with a detail derived from the rejected name")
  void unrecognisedResourceState_isPinned() throws Exception {
    assertUnrecognisedStateIsRejected(RESOURCE_STATES);
  }

  @Test
  @DisplayName("Unrecognised Negotiation event: 400 with an empty body and no content type")
  void unrecognisedNegotiationEvent_isPinned() throws Exception {
    MvcResult result = get(NEGOTIATION_EVENTS + "/NOT_AN_EVENT");
    assertEquals(400, result.getResponse().getStatus());
    assertEquals("", result.getResponse().getContentAsString());
    assertNull(result.getResponse().getContentType());
  }

  @Test
  @DisplayName("Unrecognised Resource event: 400 with an empty body and no content type")
  void unrecognisedResourceEvent_isPinned() throws Exception {
    MvcResult result = get(RESOURCE_EVENTS + "/NOT_AN_EVENT");
    assertEquals(400, result.getResponse().getStatus());
    assertEquals("", result.getResponse().getContentAsString());
    assertNull(result.getResponse().getContentType());
  }

  @Test
  @DisplayName("Case handling is asymmetric: lower-case Event names resolve, State names do not")
  void caseHandlingOfSingleItemEndpoints_isPinned() throws Exception {
    // The Event path variables go through a registered Converter that upper-cases the input; the
    // State path variables use Spring's default enum conversion, which does not.
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATION_EVENTS + "/approve"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("APPROVE"));
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_EVENTS + "/contact"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.value").value("CONTACT"));
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATION_STATES + "/submitted"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_STATES + "/submitted"))
        .andExpect(status().isBadRequest());
  }
}
