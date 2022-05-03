package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.ProjectController;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.negotiator.repository.ProjectRepository;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
public class ProjectControllerTests {
  private static final String ENDPOINT = "/v3/projects";

  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;
  @Autowired private ProjectController controller;
  @Autowired private ProjectRepository repository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    repository.deleteAll();
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenTitle_IsMissing() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setTitle(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsMissing() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setDescription(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsTooLong() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setDescription("d".repeat(513));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenEthicsVote_IsMissing() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setEthicsVote(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenEthicsVote_IsTooLong() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setEthicsVote("e".repeat(513));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenExpectedEndDate_IsMissing() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setExpectedEndDate(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenExpectedEndDate_HasWrongFormat() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);
    requestBody =
        requestBody.replace(
            "\"expectedEndDate\":\"%s\"".formatted(TestUtils.PROJECT_EXPECTED_END_DATE),
            "\"expectedEndDate\":\"13-04-2022\"");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        requestBody,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenExpectedDataGeneration_IsMissing() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    request.setExpectedDataGeneration(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  @Order(1)
  public void testCreate_Ok_whenIsTestProject_isDefault() throws Exception {
    ProjectRequest request = TestUtils.createProjectRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/v3/projects"))
                .with(httpBasic("researcher", "researcher"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.title", is(TestUtils.PROJECT_TITLE)))
        .andExpect(jsonPath("$.description", is(TestUtils.PROJECT_DESCRIPTION)))
        .andExpect(jsonPath("$.ethicsVote", is(TestUtils.PROJECT_ETHICS_VOTE)))
        .andExpect(
            jsonPath("$.expectedDataGeneration", is(TestUtils.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(jsonPath("$.expectedEndDate", is(TestUtils.PROJECT_EXPECTED_END_DATE)))
        .andExpect(jsonPath("$.isTestProject", is(TestUtils.PROJECT_IS_TEST_PROJECT)));
    assertEquals(repository.findAll().size(), 1);
  }
}
