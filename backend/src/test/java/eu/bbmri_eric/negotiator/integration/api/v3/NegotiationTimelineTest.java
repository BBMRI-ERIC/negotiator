package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
class NegotiationTimelineTest {
  @Autowired private WebApplicationContext context;
  @Autowired private NegotiationRepository negotiationRepository;
  private MockMvc mockMvc;

  @BeforeEach
  void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithUserDetails("directory")
  void getTimeline_notAuthorized_403() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/timeline"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("TheResearcher")
  @Transactional
  void getTimeline_isAuthor_ok() throws Exception {
    // Arrange: Set up the negotiation state
    Negotiation negotiation = negotiationRepository.findById("negotiation-1")
            .orElseThrow(() -> new EntityNotFoundException("Negotiation not found"));
    assertFalse(negotiation.getNegotiationResourceLifecycleRecords().isEmpty());
    negotiation.setStateForResource(
            negotiation.getResources().stream().findFirst().get().getSourceId(),
            NegotiationResourceState.RESOURCE_AVAILABLE);
    assertFalse(negotiation.getLifecycleHistory().isEmpty());

    // Act & Assert: Perform the request and verify the response
    mockMvc.perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-1/timeline"))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.timelineEvents").isArray())
            .andExpect(jsonPath("$._embedded.timelineEvents.length()").value(2))
            .andExpect(jsonPath("$._embedded.timelineEvents[0].triggeredBy").value("admin"))
            .andExpect(jsonPath("$._embedded.timelineEvents[0].text").value("admin changed the status of the Negotiation to In Progress"))
            .andExpect(jsonPath("$._embedded.timelineEvents[0].timestamp").value("2023-06-19T10:15:00"))
            .andExpect(jsonPath("$._embedded.timelineEvents[1].triggeredBy").value("TheResearcher"))
            .andExpect(jsonPath("$._embedded.timelineEvents[1].text").value("TheResearcher changed the status of biobank:1:collection:1 to Resource Available"));
  }

  @Test
  @WithMockNegotiatorUser(id = 101L, authorities = "ROLE_ADMIN")
  @Transactional
  void getTimeline_noEvents_isEmpty() throws Exception {
    assertTrue(
        negotiationRepository
            .findById("negotiation-2")
            .orElseThrow(() -> new EntityNotFoundException("idk"))
            .getLifecycleHistory()
            .isEmpty());
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/negotiation-2/timeline"))
        .andDo(print())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }
}
