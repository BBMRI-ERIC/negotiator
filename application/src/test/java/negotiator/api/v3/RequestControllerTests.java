package negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.bbmri.eric.csit.service.model.Project;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.model.Request;
import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.v3.RequestController;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.repository.ProjectRepository;
import eu.bbmri.eric.csit.service.repository.QueryRepository;
import eu.bbmri.eric.csit.service.repository.RequestRepository;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.modelmapper.ModelMapper;
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
@TestMethodOrder(OrderAnnotation.class)
public class RequestControllerTests {
  private MockMvc mockMvc;

  @Autowired private WebApplicationContext context;
  @Autowired private RequestController requestController;
  @Autowired private ProjectRepository projectRepository;
  @Autowired private RequestRepository requestRepository;
  @Autowired private QueryRepository queryRepository;
  @Autowired private ModelMapper modelMapper;

  private static final String TITLE = "request title";
  private static final String DESCRIPTION = "request description";
  private static final String REQUESTS_ENDPOINT = "/v3/requests";
  private static final String PROJECTS_ENDPOINT = "/v3/projects/%s/requests";

  private Query testQuery;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    QueryRequest queryRequest = TestObjectFactory.createQueryRequest(false);
    Query query = modelMapper.map(queryRequest, Query.class);
    testQuery = queryRepository.save(query);
  }

  @AfterEach
  public void after() {
    queryRepository.deleteAll();
    requestRepository.deleteAll();
    projectRepository.deleteAll();
  }

  private RequestRequest createRequest(boolean update, boolean includeProject) {
    String suffix = update ? "u" : "";

    RequestRequest.RequestRequestBuilder builder =
        RequestRequest.builder()
            .title(String.format("%s%s", TITLE, suffix))
            .description(String.format("%s%s", DESCRIPTION, suffix))
            .queries(List.of(testQuery.getId()));
    if (includeProject) {
      builder.project(TestObjectFactory.createProjectRequest(update));
    }
    return builder.build();
  }

  private String jsonFromRequest(Object request) throws JsonProcessingException {
    ObjectMapper mapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
    return mapper.writeValueAsString(request);
  }

  private void checkErrorResponse(
      HttpMethod method,
      RequestRequest request,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth,
      String endpoint)
      throws Exception {
    String requestBody = jsonFromRequest(request);
    checkErrorResponse(method, requestBody, statusMatcher, auth, endpoint);
  }

  private void checkErrorResponse(
      HttpMethod method,
      String requestBody,
      ResultMatcher statusMatcher,
      RequestPostProcessor auth,
      String endpoint)
      throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.request(method, URI.create(endpoint))
                .with(auth)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(statusMatcher);
    assertEquals(requestRepository.findAll().size(), 0);
  }

  @Test
  public void testGetAll_Unauthorized_whenNoAuth() throws Exception {
    checkErrorResponse(
        HttpMethod.GET, "", status().isUnauthorized(), anonymous(), REQUESTS_ENDPOINT);

    checkErrorResponse(
        HttpMethod.GET, "", status().isUnauthorized(), anonymous(), PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Unauthorized_whenWrongAuth() throws Exception {
    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        REQUESTS_ENDPOINT);

    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Forbidden_whenNoPermission() throws Exception {
    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        REQUESTS_ENDPOINT);

    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Ok() throws Exception {
    RequestRequest request = createRequest(false, true);
    Project projectEntity = modelMapper.map(request.getProject(), Project.class);
    projectRepository.save(projectEntity);
    Request entity = modelMapper.map(request, Request.class);
    entity.setProject(projectEntity);
    entity = requestRepository.save(entity);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REQUESTS_ENDPOINT)
                .with(httpBasic("researcher", "researcher")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id", is(entity.getId().intValue())))
        .andExpect(jsonPath("$[0].title", is(TITLE)))
        .andExpect(jsonPath("$[0].description", is(DESCRIPTION)))
        .andExpect(jsonPath("$[0].token").isString())
        .andExpect(jsonPath("$[0].project.id", is(projectEntity.getId().intValue())))
        .andExpect(jsonPath("$[0].project.title", is(TestObjectFactory.PROJECT_TITLE)))
        .andExpect(jsonPath("$[0].project.description", is(TestObjectFactory.PROJECT_DESCRIPTION)))
        .andExpect(
            jsonPath(
                "$[0].project.expectedDataGeneration",
                is(TestObjectFactory.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(
            jsonPath(
                "$[0].project.expectedEndDate", is(TestObjectFactory.PROJECT_EXPECTED_END_DATE)))
        .andExpect(
            jsonPath("$[0].project.isTestProject", is(TestObjectFactory.PROJECT_IS_TEST_PROJECT)));
  }

  @Test
  public void testGetAll_Ok_whenEmptyResult() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REQUESTS_ENDPOINT)
                .with(httpBasic("researcher", "researcher")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  public void testGetById_Unauthorized_whenNoAuth() throws Exception {
    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Unauthorized_whenWrongAuth() throws Exception {
    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Forbidden_whenNoPermission() throws Exception {
    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_NotFound_whenWrongId() throws Exception {
    checkErrorResponse(
        HttpMethod.GET,
        "",
        status().isNotFound(),
        httpBasic("researcher", "researcher"),
        "%s/-1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Ok_whenCorrectId() throws Exception {
    RequestRequest request = createRequest(false, true);
    Project projectEntity = modelMapper.map(request.getProject(), Project.class);
    projectRepository.save(projectEntity);
    Request entity = modelMapper.map(request, Request.class);
    entity.setProject(projectEntity);
    entity = requestRepository.save(entity);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/%s".formatted(REQUESTS_ENDPOINT, entity.getId()))
                .with(httpBasic("researcher", "researcher")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(entity.getId().intValue())))
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.project.id", is(projectEntity.getId().intValue())))
        .andExpect(jsonPath("$.project.title", is(TestObjectFactory.PROJECT_TITLE)))
        .andExpect(jsonPath("$.project.description", is(TestObjectFactory.PROJECT_DESCRIPTION)))
        .andExpect(
            jsonPath(
                "$.project.expectedDataGeneration",
                is(TestObjectFactory.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(
            jsonPath("$.project.expectedEndDate", is(TestObjectFactory.PROJECT_EXPECTED_END_DATE)))
        .andExpect(
            jsonPath("$.project.isTestProject", is(TestObjectFactory.PROJECT_IS_TEST_PROJECT)));
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    RequestRequest request = createRequest(false, true);
    checkErrorResponse(
        HttpMethod.POST, request, status().isUnauthorized(), anonymous(), REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        anonymous(),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    RequestRequest request = createRequest(false, true);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    RequestRequest request = createRequest(false, true);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_NotFound_whenProjectDoesNotExist() throws Exception {
    RequestRequest request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isNotFound(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(-1));
  }

  @Test
  public void testCreate_BadRequest_whenTitle_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.setTitle(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    request.setTitle(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.setDescription(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    request.setDescription(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenQueries_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.setQueries(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    request.setQueries(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenQueries_IsEmpty() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.setQueries(Collections.emptyList());
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);

    request = createRequest(false, false);
    request.setQueries(Collections.emptyList());
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenSomeQueries_IsNotFound() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.setQueries(List.of(-1L));
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);

    // Create the project before
    Project projectEntity =
        modelMapper.map(TestObjectFactory.createProjectRequest(false), Project.class);
    projectRepository.save(projectEntity);

    request = createRequest(false, false);
    request.setQueries(List.of(-1L));
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(projectEntity.getId()));
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsTooLong() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.setDescription("d".repeat(513));
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);

    // Create the project before
    Project projectEntity =
        modelMapper.map(TestObjectFactory.createProjectRequest(false), Project.class);
    projectRepository.save(projectEntity);

    request = createRequest(false, false);
    request.setDescription("d".repeat(513));
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        PROJECTS_ENDPOINT.formatted(projectEntity.getId()));
  }

  @Test
  public void testCreate_BadRequest_whenProjectTitle_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setTitle(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectDescription_IsTooLong() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setDescription("d".repeat(513));
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectDescription_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setDescription(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectEthicsVote_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setEthicsVote(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectEthicsVote_IsTooLong() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setEthicsVote("d".repeat(513));
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectExpectedEndDate_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setExpectedEndDate(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectExpectedEndDate_HasWrongFormat() throws Exception {
    RequestRequest request = createRequest(false, true);
    String requestBody = jsonFromRequest(request);
    requestBody =
        requestBody.replace(
            "\"expectedEndDate\":\"%s\"".formatted(TestObjectFactory.PROJECT_EXPECTED_END_DATE),
            "\"expectedEndDate\":\"13-04-2022\"");
    checkErrorResponse(
        HttpMethod.POST,
        requestBody,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectExpectedDataGeneration_IsMissing() throws Exception {
    RequestRequest request = createRequest(false, true);
    request.getProject().setExpectedDataGeneration(null);
    checkErrorResponse(
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("researcher", "researcher"),
        REQUESTS_ENDPOINT);
  }

  @Test
  @Order(1)
  public void testCreate_Ok_whenProjectIsIncluded() throws Exception {
    RequestRequest request = createRequest(false, true);
    String requestBody = jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(REQUESTS_ENDPOINT))
                .with(httpBasic("researcher", "researcher"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.queries[0].id", is(testQuery.getId().intValue())))
        .andExpect(jsonPath("$.project.id", is(1)))
        .andExpect(jsonPath("$.project.title", is(TestObjectFactory.PROJECT_TITLE)))
        .andExpect(jsonPath("$.project.description", is(TestObjectFactory.PROJECT_DESCRIPTION)))
        .andExpect(
            jsonPath(
                "$.project.expectedDataGeneration",
                is(TestObjectFactory.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(
            jsonPath("$.project.expectedEndDate", is(TestObjectFactory.PROJECT_EXPECTED_END_DATE)))
        .andExpect(
            jsonPath("$.project.isTestProject", is(TestObjectFactory.PROJECT_IS_TEST_PROJECT)));
    assertEquals(requestRepository.findAll().size(), 1);
    assertEquals(projectRepository.findAll().size(), 1);
  }

  @Test
  @Order(2)
  public void testCreate_Ok_whenProjectAlreadyExists() throws Exception {
    // Create the project before
    Project projectEntity =
        modelMapper.map(TestObjectFactory.createProjectRequest(false), Project.class);
    projectRepository.save(projectEntity);

    RequestRequest request = createRequest(false, true);
    String requestBody = jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    URI.create(PROJECTS_ENDPOINT.formatted(projectEntity.getId())))
                .with(httpBasic("researcher", "researcher"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.token").isString())
        .andExpect(jsonPath("$.queries[0].id", is(testQuery.getId().intValue())));
    assertEquals(requestRepository.findAll().size(), 1);
    assertEquals(projectRepository.findAll().size(), 1);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    RequestRequest request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    RequestRequest request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    RequestRequest request = createRequest(false, false);
    checkErrorResponse(
        HttpMethod.PUT,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testUpdate_Ok_whenChangeTitle() throws Exception {
    // The data source to be updated
    Request requestEntity = modelMapper.map(createRequest(false, false), Request.class);
    requestRepository.save(requestEntity);

    testQuery.setRequest(requestEntity);
    queryRepository.save(testQuery);

    // Request body with updated values
    RequestRequest request = createRequest(true, false);
    String requestBody = jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("%s/%s".formatted(REQUESTS_ENDPOINT, requestEntity.getId()))
                .with(httpBasic("researcher", "researcher"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
