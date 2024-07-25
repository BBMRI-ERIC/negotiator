package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.dto.InformationRequirementCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InformationRequirementControllerTest {
  private final String INFO_REQUIREMENT_ENDPOINT = "/v3/info-requirements";
  private MockMvc mockMvc;

  @BeforeEach
  void setup(WebApplicationContext wac) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
  }

  @Test
  @WithMockUser
  void createInformationRequirement_null_returns400() throws Exception {
    InformationRequirementCreateDTO createDTO = new InformationRequirementCreateDTO(null, null);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title", is("Encountered a null pointer exception")));
  }

  @Test
  @WithMockUser
  void createInformationRequirement_correctBody_ok() throws Exception {
    InformationRequirementCreateDTO createDTO =
        new InformationRequirementCreateDTO(1L, NegotiationResourceEvent.CONTACT);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(INFO_REQUIREMENT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(createDTO)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.forResourceEvent", is("CONTACT")))
        .andExpect(jsonPath("$._links").isNotEmpty());
  }
}
