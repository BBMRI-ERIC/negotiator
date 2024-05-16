package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
public class NetworkControllerTests {

  private static final String NETWORKS_URL = "/v3/networks";

  @Autowired private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testGetAll_Networks_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.networks.length()", is(3)))
        .andExpect(jsonPath("$._embedded.networks.[0].id", is(1)))
        .andExpect(jsonPath("$._embedded.networks.[1].id", is(2)))
        .andExpect(jsonPath("$._embedded.networks.[2].id", is(3)));
  }

  @Test
  public void testGet_Network_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.id", is(1)));
  }

  @Test
  public void testGet_resources_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/resources"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(3)))
        .andExpect(jsonPath("$._embedded.resources.length()", is(3)))
        .andExpect(jsonPath("$._embedded.resources.[0].id", is("4")))
        .andExpect(jsonPath("$._embedded.resources.[1].id", is("5")))
        .andExpect(jsonPath("$._embedded.resources.[2].id", is("6")));
  }

  @Test
  public void testGet_managers_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/managers"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/json"))
        .andExpect(jsonPath("$.[0].id", is("101")))
        .andExpect(jsonPath("$.[1].id", is("102")));
  }

  @Test
  public void testGet_negotiations_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(NETWORKS_URL + "/1/negotiations"))
        .andExpect(status().isOk())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.page.totalElements", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.length()", is(4)))
        .andExpect(jsonPath("$._embedded.negotiations.[0].id", is("negotiation-1")))
        .andExpect(jsonPath("$._embedded.negotiations.[1].id", is("negotiation-3")))
        .andExpect(jsonPath("$._embedded.negotiations.[2].id", is("negotiation-4")))
        .andExpect(jsonPath("$._embedded.negotiations.[3].id", is("negotiation-5")));
  }
}
