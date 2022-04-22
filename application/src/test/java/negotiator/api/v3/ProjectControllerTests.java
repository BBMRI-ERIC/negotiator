package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.ProjectController;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.repository.ProjectRepository;
import java.net.URI;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class ProjectControllerTests {
  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;
  @Autowired private ProjectController controller;
  @Autowired private ProjectRepository repository;

  private static final String TITLE = "project title";
  private static final String DESCRIPTION = "project description";
  private static final String ETHICS_VOTE = "ethics vote";
  private static final String EXPECTED_END_DATE = "2022-04-13";
  private static final boolean EXPECTED_DATA_GENERATION = true;
  private static final boolean IS_TEST_PROJECT = true;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    repository.deleteAll();
  }

  private ProjectRequest createRequest(boolean update) {
    String suffix = update ? "u" : "";

    return ProjectRequest.builder()
        .title(String.format("%s%s", TITLE, suffix))
        .description(String.format("%s%s", DESCRIPTION, suffix))
        .ethicsVote(String.format("%s%s", ETHICS_VOTE, suffix))
        .expectedEndDate(LocalDate.parse(EXPECTED_END_DATE))
        .expectedDataGeneration(update)
        .isTestProject(IS_TEST_PROJECT)
        .build();
  }

  private String jsonFromRequest(Object request) throws JsonProcessingException {
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    return mapper.writeValueAsString(request);
  }

  private void checkErrorResponse(
      HttpMethod method,
      ProjectRequest request,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth)
      throws Exception {
    String requestBody = jsonFromRequest(request);
    checkErrorResponse(method, requestBody, statusMatcher, auth);
  }

  private void checkErrorResponse(
      HttpMethod method, String requestBody, ResultMatcher statusMatcher, RequestPostProcessor auth)
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create("/v3/projects"))
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    assertEquals(repository.findAll().size(), 0);
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    ProjectRequest request = createRequest(false);
    checkErrorResponse(HttpMethod.POST, request, status().isUnauthorized(), anonymous());
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    ProjectRequest request = createRequest(false);
    checkErrorResponse(
        HttpMethod.POST, request, status().isUnauthorized(), httpBasic("researcher", "wrong_pass"));
  }

  @Test
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    ProjectRequest request = createRequest(false);
    checkErrorResponse(
        HttpMethod.POST, request, status().isForbidden(), httpBasic("directory", "directory"));
  }

  @Test
  public void testBadRequest_whenTitle_IsMissing() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setTitle(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenDescription_IsMissing() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setDescription(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenDescription_IsTooLong() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setDescription("d".repeat(513));
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenEthicsVote_IsMissing() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setEthicsVote(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenEthicsVote_IsTooLong() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setEthicsVote("e".repeat(513));
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenExpectedEndDate_IsMissing() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setExpectedEndDate(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenExpectedEndDate_HasWrongFormat() throws Exception {
    ProjectRequest request = createRequest(false);
    String requestBody = jsonFromRequest(request);
    requestBody =
        requestBody.replace(
            "\"expectedEndDate\":\"%s\"".formatted(EXPECTED_END_DATE),
            "\"expectedEndDate\":\"13-04-2022\"");
    checkErrorResponse(
        HttpMethod.POST,
        requestBody,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"));
  }

  @Test
  public void testBadRequest_whenExpectedDataGeneration_IsMissing() throws Exception {
    ProjectRequest request = createRequest(false);
    request.setExpectedDataGeneration(null);
    checkErrorResponse(
        HttpMethod.POST, request, status().isBadRequest(), httpBasic("researcher", "researcher"));
  }

  @Test
  public void testCreated_whenIsTestProject_isDefault() throws Exception {
    ProjectRequest request = createRequest(false);
    String requestBody = jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create("/v3/projects"))
                .with(httpBasic("researcher", "researcher"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(1)))
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.ethicsVote", is(ETHICS_VOTE)))
        .andExpect(jsonPath("$.expectedDataGeneration", is(false)))
        .andExpect(jsonPath("$.expectedEndDate", is(EXPECTED_END_DATE)))
        .andExpect(jsonPath("$.isTestProject", is(IS_TEST_PROJECT)));
    assertEquals(repository.findAll().size(), 1);
  }
}
