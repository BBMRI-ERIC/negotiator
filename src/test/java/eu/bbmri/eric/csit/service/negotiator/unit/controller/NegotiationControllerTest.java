package eu.bbmri.eric.csit.service.negotiator.unit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NegotiationControllerTest {

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;

  @Mock private NegotiationService negotiationService;

  @Mock private NegotiatorUserDetailsService negotiatorUserDetailsService;

  @BeforeEach
  public void before() {
    MockitoAnnotations.openMocks(this); // Initialize mocks

    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mockStatic(NegotiatorUserDetailsService.class);
  }

  @AfterEach
  public void after() {
    Mockito.clearAllCaches();
  }

  private static final String NEGOTIATIONS_URL = "/v3/negotiations";

  @Test
  public void testListNegotiations() throws Exception {
    // Mock the service method to return an empty list
    when(negotiationService.findAllNegotiationsCreatedBy(any()))
        .thenReturn(Collections.emptyList());

    // Perform the HTTP request and validate the response
    mockMvc.perform(post(NEGOTIATIONS_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(authorities = "ROLE_USER") // Assuming a non-admin user
  void sendEvent_NonAdmin_Forbidden() throws Exception {
    when(negotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin()).thenReturn(false);

    mockMvc
        .perform(put("%s/negotiation-1/lifecycle/APPROVE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_InvalidEvent_BadRequest() throws Exception {
    when(negotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin()).thenReturn(true);
    when(negotiationService.findById("negotiation-1", false)).thenReturn(new NegotiationDTO());

    mockMvc
        .perform(put("%s/negotiation-1/lifecycle/NONE_EXISTING_VALUE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(authorities = "ROLE_ADMIN")
  void sendEvent_ValidInput_ReturnNegotiationState() throws Exception {
    when(negotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin()).thenReturn(true);
    when(negotiationService.findById("negotiation-1", false)).thenReturn(new NegotiationDTO());

    mockMvc
        .perform(put("%s/negotiation-1/lifecycle/APPROVE".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk());
  }
}
