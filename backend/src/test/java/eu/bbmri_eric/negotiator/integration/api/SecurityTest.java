package eu.bbmri_eric.negotiator.integration.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class SecurityTest {

  @Autowired private WebApplicationContext context;

  @Autowired private UserDetailsService userDetailsService;

  @Value("#{'${spring.security.cors.allowed-methods}'.split(',')}")
  private List<String> ALLOWED_METHODS;

  @Value("#{'${spring.security.cors.allowed-origins}'.split(',')}")
  private List<String> ALLOWED_ORIGINS;

  @Value("#{'${spring.security.cors.allowed-headers}'.split(',')}")
  private List<String> ALLOWED_HEADERS;

  @Value("${spring.security.cors.allow-credentials}")
  private boolean ALLOW_CREDENTIALS;

  @Value("${spring.security.cors.max-age}")
  private Long MAX_AGE = 3600L;

  private MockMvc mockMvc;

  @BeforeEach
  public void beforeAll() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void testUnauthenticatedUser() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithUserDetails("TheResearcher")
  void testAuthorized() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/users/%s/negotiations"
                    .formatted(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId())))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(username = "testName")
  void testGetAuthenticatedUserName() {
    assertEquals("testName", SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @Test
  void testGetUnauthenticatedUserName() {
    assertThrows(
        NullPointerException.class,
        () -> SecurityContextHolder.getContext().getAuthentication().getName());
  }

  @Test
  void testCreateQueryWithBasicAuth() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/directory/create_query")
                .contentType(MediaType.APPLICATION_JSON)
                .with(httpBasic("directory", "directory")))
        .andExpect(status().isBadRequest());
  }

  @Test
  void testSubStringBetween() {
    String full =
        "urn:geant:bbmri-eric.eu:group:bbmri:collections:BBMRI-ERIC%20Directory:bbmri-eric.ID.CZ_MMCI.collection.LTS#perun.bbmri-eric.eu";
    String sub = StringUtils.substringBetween(full, "Directory:", "#perun").replace(".", ":");
    assertEquals("bbmri-eric:ID:CZ_MMCI:collection:LTS", sub);
  }

  @Test
  public void corsHeadersArePresent() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.options("/v3/organizations")
                .header("Access-Control-Request-Method", "GET")
                .header("Origin", "http://localhost:8087")
                .header("Access-Control-Request-Headers", "X-Requested-With"))
        .andExpect(status().isOk())
        .andExpect(
            MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Origin", String.join(",", ALLOWED_ORIGINS)))
        .andExpect(
            MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Methods", String.join(",", ALLOWED_METHODS)))
        .andExpect(
            MockMvcResultMatchers.header()
                .string("Access-Control-Allow-Headers", "X-Requested-With"))
        .andExpect(
            MockMvcResultMatchers.header().string("Access-Control-Max-Age", MAX_AGE.toString()));
  }

  @Test
  void getInfoEndpoint_noAuth_200() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/actuator/info")).andExpect(status().isOk());
  }
}
