package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class LifecycleInfoTests {
  private MockMvc mockMvc;
  private final String NEGOTIATION_STATES_ENDPOINT = "/v3/negotiation-lifecycle/states";
  private final String NEGOTIATION_EVENTS_ENDPOINT = "/v3/negotiation-lifecycle/events";
  private final String RESOURCE_EVENTS_ENDPOINT = "/v3/resource-lifecycle/events";
  private final String RESOURCE_STATES_ENDPOINT = "/v3/resource-lifecycle/states";
  @Autowired private WebApplicationContext context;
  @Autowired private ModelMapper modelMapper;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithUserDetails("researcher")
  void getAllNegotiationStates_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATION_STATES_ENDPOINT))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.states").isArray())
        .andExpect(jsonPath("$._embedded.states[0].value").isString())
        .andExpect(jsonPath("$._embedded.states[0].label").isString())
        .andExpect(jsonPath("$._embedded.states[0].description").isString());
  }

  @Test
  @WithUserDetails("researcher")
  void getAllNegotiationEvents_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NEGOTIATION_EVENTS_ENDPOINT))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.events").isArray())
        .andExpect(jsonPath("$._embedded.events[0].value").isString())
        .andExpect(jsonPath("$._embedded.events[0].label").isString())
        .andExpect(jsonPath("$._embedded.events[0].description").isString());
  }

  @Test
  @WithUserDetails("researcher")
  void getAllResourceStates_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_STATES_ENDPOINT))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.states").isArray())
        .andExpect(jsonPath("$._embedded.states[0].value").isString())
        .andExpect(jsonPath("$._embedded.states[0].label").isString())
        .andExpect(jsonPath("$._embedded.states[0].description").isString());
  }

  @Test
  @WithUserDetails("researcher")
  void getAllResourceEvents_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_EVENTS_ENDPOINT))
        .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._embedded.events").isArray())
        .andExpect(jsonPath("$._embedded.events[0].value").isString())
        .andExpect(jsonPath("$._embedded.events[0].label").isString())
        .andExpect(jsonPath("$._embedded.events[0].description").isString());
  }
}
