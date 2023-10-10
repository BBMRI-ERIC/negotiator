package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import lombok.extern.apachecommons.CommonsLog;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@CommonsLog
public class UserControllerTest {
  private static final String ROLES_ENDPOINT = "/v3/users/roles";
  @Autowired private WebApplicationContext context;
  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser
  void getInfo_mockUserNoAuthorities_Ok() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(ROLES_ENDPOINT)).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(authorities = "biobank:1:collection:1")
  void getInfo_mockUserOneAuthority_responseIsOk() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ROLES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.[0]", is("biobank:1:collection:1")));
  }

  @Test
  @WithMockUser(authorities = {"biobank:1:collection:1", "ROLE_RESEARCHER"})
  void getInfo_mockUserMultipleAuthorities_responseIsOk() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ROLES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath("$")
                .value(Matchers.containsInAnyOrder("biobank:1:collection:1", "ROLE_RESEARCHER")));
  }
}
