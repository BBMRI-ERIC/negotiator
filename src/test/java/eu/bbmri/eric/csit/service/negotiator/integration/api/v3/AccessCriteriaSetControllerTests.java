package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.ProjectController;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class AccessCriteriaSetControllerTests {

  private static final String ENDPOINT = "/v3/access-criteria";
  private static final String CORRECT_TOKEN_VALUE = "researcher";
  private static final String FORBIDDEN_TOKEN_VALUE = "unknown";
  private static final String UNAUTHORIZED_TOKEN_VALUE = "unauthorized";

  private MockMvc mockMvc;

  @Autowired
  private WebApplicationContext context;
  @Autowired
  private ProjectController controller;
  @Autowired
  private ProjectRepository repository;
  @Autowired
  private ModelMapper modelMapper;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    repository.deleteAll();
  }

  @Test
  public void testGet_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testGet_Unauthorized_whenWrongAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"), ENDPOINT);
  }

  @Test
  public void testGet_Forbidden_whenNoPermission() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isForbidden(), httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testGet_BadRequest_whenMissingResourceId() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isBadRequest(), httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testGet_NotFound_whenResourceIdIsNotExistent() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isNotFound(), httpBasic("researcher", "researcher"),
        "%s?resourceId=UNKNOWN".formatted(ENDPOINT));
  }

  @Test
  public void testGet_Ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders
                .get(ENDPOINT)
                .param("resourceId", "biobank:1")
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sections[0].accessCriteria").isArray())
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].name", is("title")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].label", is("Title")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].description", is("Give a title")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].type", is("text")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[0].required", is(true)))
        .andExpect(jsonPath("$.sections[0].accessCriteria[1].name", is("description")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[1].label", is("Description")))
        .andExpect(
            jsonPath("$.sections[0].accessCriteria[1].description", is("Give a description")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[1].type", is("textarea")))
        .andExpect(jsonPath("$.sections[0].accessCriteria[1].required", is(false)))
        .andExpect(jsonPath("$.sections[1].accessCriteria[0].name", is("num-of-subjects")))
        .andExpect(jsonPath("$.sections[1].accessCriteria[0].label", is("Number of subjects")))
        .andExpect(
            jsonPath("$.sections[1].accessCriteria[0].description", is("Number of biosamples")))
        .andExpect(jsonPath("$.sections[1].accessCriteria[0].type", is("number")))
        .andExpect(jsonPath("$.sections[1].accessCriteria[0].required", is(true)));
  }
}
