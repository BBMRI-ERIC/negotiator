package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
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
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isOk(), anonymous(), NETWORKS_URL);
  }

  @Test
  public void testGet_Network_ok() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "/1", status().isOk(), anonymous(), NETWORKS_URL);
  }

  @Test
  public void testGet_resources_ok() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "/1/resources", status().isOk(), anonymous(), NETWORKS_URL);
  }

  @Test
  public void testGet_managers_ok() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "/1/managers", status().isOk(), anonymous(), NETWORKS_URL);
  }
}
