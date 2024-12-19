package eu.bbmri_eric.negotiator.unit.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.governance.network.NetworkController;
import eu.bbmri_eric.negotiator.governance.network.NetworkModelAssembler;
import eu.bbmri_eric.negotiator.governance.network.NetworkService;
import eu.bbmri_eric.negotiator.governance.network.NetworkStatisticsService;
import eu.bbmri_eric.negotiator.governance.network.SimpleNetworkStatistics;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationModelAssembler;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserModelAssembler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(NetworkController.class)
public class NetworkControllerTest {
  @Autowired private MockMvc mockMvc;

  @MockBean private NetworkService networkService;

  @MockBean private PersonService personService;

  @MockBean private ResourceService resourceService;

  @MockBean private NegotiationService negotiationService;

  @MockBean private NetworkModelAssembler networkModelAssembler;

  @MockBean private ResourceModelAssembler resourceModelAssembler;

  @MockBean private NegotiationModelAssembler negotiationModelAssembler;

  @MockBean private UserModelAssembler userModelAssembler;

  @MockBean private NetworkStatisticsService networkStatisticsService;

  @Test
  @WithMockUser
  void getNetworkStats_noPeriod_400() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/networks/1/statistics"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void getNetworkStats_noUntil_400() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/networks/1/statistics?since=01-01-2024"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void getNetworkStats_wrongDateFormat_400() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/networks/1/statistics?since=2024&until=01-01-2024"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/networks/1/statistics?since=202-01&until=01-01-2024"))
        .andExpect(status().isBadRequest());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/networks/1/statistics?since=01-2024&until=01-01-2024"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  void getNetworkStats_correctDateFormat_200() throws Exception {
    when(networkStatisticsService.getBasicNetworkStats(1L))
        .thenReturn(new SimpleNetworkStatistics());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/networks/1/statistics?since=2024-11-18&until=2024-12-18"))
        .andExpect(status().isOk());
  }
}
