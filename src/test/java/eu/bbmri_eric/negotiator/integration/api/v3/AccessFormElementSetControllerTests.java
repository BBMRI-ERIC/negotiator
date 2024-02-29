package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.api.controller.v3.ProjectController;
import eu.bbmri_eric.negotiator.database.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AccessFormElementSetControllerTests {

  private static final String ENDPOINT = "/v3/access-criteria";
  private static final String CORRECT_TOKEN_VALUE = "researcher";
  private static final String FORBIDDEN_TOKEN_VALUE = "unknown";
  private static final String UNAUTHORIZED_TOKEN_VALUE = "unauthorized";

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;
  @Autowired private ProjectController controller;
  @Autowired private ProjectRepository repository;
  @Autowired private ModelMapper modelMapper;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    repository.deleteAll();
  }

  @Test
  public void testGet_Unauthorized_whenWrongAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testGet_BadRequest_whenMissingResourceId() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testGet_NotFound_whenResourceIdIsNotExistent() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isNotFound(),
        httpBasic("researcher", "researcher"),
        "%s?resourceId=UNKNOWN".formatted(ENDPOINT));
  }

  @Test
  public void testGet_Ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ENDPOINT).param("resourceId", "biobank:1:collection:1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].accessCriteria").isArray())
        .andExpect(jsonPath("$.sections[0].name", is("project")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].name", is("title")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].required", is(true)))
        .andExpect(jsonPath("$.sections[0].accessCriteria[1].name", is("description")))
        .andExpect(jsonPath("$.sections[1].name", is("request")))
        .andExpect(jsonPath("$.sections[1].accessCriteria[0].name", is("description")))
        .andExpect(jsonPath("$.sections[2].name", is("ethics-vote")));
  }
}
