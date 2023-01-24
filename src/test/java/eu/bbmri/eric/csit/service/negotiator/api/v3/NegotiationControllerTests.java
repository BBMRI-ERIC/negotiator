package eu.bbmri.eric.csit.service.negotiator.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Project;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ProjectRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
import java.net.URI;
import java.util.Collections;
import java.util.Set;
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
public class NegotiationControllerTests {

  private static final String TITLE = "negotiation title";
  private static final String DESCRIPTION = "negotiation description";
  private static final String REQUESTS_ENDPOINT = "/v3/negotiations";
  private static final String PROJECTS_ENDPOINT = "/v3/projects/%s/negotiations";
  private static final String CORRECT_TOKEN_VALUE = "researcher";
  private static final String FORBIDDEN_TOKEN_VALUE = "unknown";
  private static final String UNAUTHORIZED_TOKEN_VALUE = "unauthorized";

  @Autowired
  private WebApplicationContext context;
  @Autowired
  private NegotiationController negotiationController;
  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private NegotiationRepository negotiationRepository;
  @Autowired
  private RequestRepository requestRepository;
  @Autowired
  private ModelMapper modelMapper;
  @Autowired
  private RequestService requestService;

  private MockMvc mockMvc;
  private Request testRequest;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    testRequest = createQueryEntity();
  }

  @AfterEach
  public void after() {
    requestRepository.deleteAll();
    negotiationRepository.deleteAll();
    projectRepository.deleteAll();
  }

  private Request createQueryEntity() {
    RequestCreateDTO queryRequest = TestUtils.createRequest(false);
    return requestService.create(queryRequest);
  }

  @Test
  public void testGetAll_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), anonymous(), REQUESTS_ENDPOINT);

    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        anonymous(),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        REQUESTS_ENDPOINT);

    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Unauthorized_whenInvalidToken() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), "", REQUESTS_ENDPOINT);

    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), "", PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Forbidden_whenNoPermission() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        FORBIDDEN_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        FORBIDDEN_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Forbidden_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        REQUESTS_ENDPOINT);

    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testGetAll_Ok() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    Negotiation entity = modelMapper.map(request, Negotiation.class);
    entity = negotiationRepository.save(entity);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REQUESTS_ENDPOINT)
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id", is(entity.getId())))
        .andExpect(jsonPath("$[0].title", is(TITLE)))
        .andExpect(jsonPath("$[0].description", is(DESCRIPTION)))
        .andExpect(jsonPath("$[0].project.id", is(entity.getProject().getId())))
        .andExpect(jsonPath("$[0].project.title", is(TestUtils.PROJECT_TITLE)))
        .andExpect(jsonPath("$[0].project.description", is(TestUtils.PROJECT_DESCRIPTION)))
        .andExpect(
            jsonPath(
                "$[0].project.expectedDataGeneration",
                is(TestUtils.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(
            jsonPath("$[0].project.expectedEndDate", is(TestUtils.PROJECT_EXPECTED_END_DATE)))
        .andExpect(jsonPath("$[0].project.isTestProject", is(TestUtils.PROJECT_IS_TEST_PROJECT)));
  }

  @Test
  public void testGetAll_Ok_whenEmptyResult() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(REQUESTS_ENDPOINT)
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  public void testGetById_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Forbidden_whenNoPermissionAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        FORBIDDEN_TOKEN_VALUE,
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Forbidden_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_NotFound_whenWrongId() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isNotFound(),
        CORRECT_TOKEN_VALUE,
        "%s/-1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testGetById_Ok_whenCorrectId() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    Negotiation entity = modelMapper.map(request, Negotiation.class);
    entity = negotiationRepository.save(entity);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/%s".formatted(REQUESTS_ENDPOINT, entity.getId()))
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(entity.getId())))
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.project.id", is(entity.getProject().getId())))
        .andExpect(jsonPath("$.project.title", is(TestUtils.PROJECT_TITLE)))
        .andExpect(jsonPath("$.project.description", is(TestUtils.PROJECT_DESCRIPTION)))
        .andExpect(
            jsonPath(
                "$.project.expectedDataGeneration", is(TestUtils.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(jsonPath("$.project.expectedEndDate", is(TestUtils.PROJECT_EXPECTED_END_DATE)))
        .andExpect(jsonPath("$.project.isTestProject", is(TestUtils.PROJECT_IS_TEST_PROJECT)));
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        anonymous(),
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        anonymous(),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_NotFound_whenProjectDoesNotExist() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isNotFound(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(-1));
  }

  @Test
  public void testCreate_BadRequest_whenTitle_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.setTitle(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    request.setTitle(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.setDescription(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    request.setDescription(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenQueries_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.setQueries(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    request.setQueries(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenQueries_IsEmpty() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.setQueries(Collections.emptySet());
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    request.setQueries(Collections.emptySet());
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(1));
  }

  @Test
  public void testCreate_BadRequest_whenSomeQueries_IsNotFound() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.setQueries(Set.of("unknownn"));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    // Create the project before
    Project projectEntity = modelMapper.map(TestUtils.createProjectRequest(false), Project.class);
    projectRepository.save(projectEntity);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    request.setQueries(Set.of("unknown"));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(projectEntity.getId()));
  }

  @Test
  public void testCreate_BadRequest_whenDescription_IsTooLong() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.setDescription("d".repeat(513));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);

    // Create the project before
    Project projectEntity = modelMapper.map(TestUtils.createProjectRequest(false), Project.class);
    projectRepository.save(projectEntity);

    request = TestUtils.createNegotiation(false, false, Set.of(testRequest.getId()));
    request.setDescription("d".repeat(513));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        PROJECTS_ENDPOINT.formatted(projectEntity.getId()));
  }

  @Test
  public void testCreate_BadRequest_whenProjectTitle_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setTitle(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectDescription_IsTooLong() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setDescription("d".repeat(513));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectDescription_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setDescription(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectEthicsVote_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setEthicsVote(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectEthicsVote_IsTooLong() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setEthicsVote("d".repeat(513));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectExpectedEndDate_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setExpectedEndDate(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectExpectedEndDate_HasWrongFormat() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
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
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenProjectExpectedDataGeneration_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    request.getProject().setExpectedDataGeneration(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        CORRECT_TOKEN_VALUE,
        REQUESTS_ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenQuery_IsAlreadyAssignedToAnotherRequest() throws Exception {
    NegotiationCreateDTO createRequest = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    // The data source to be updated
    Negotiation negotiationEntity = modelMapper.map(createRequest, Negotiation.class);
    negotiationRepository.save(negotiationEntity);

    testRequest.setNegotiation(negotiationEntity);
    requestRepository.save(testRequest);
    assertEquals(1, negotiationRepository.count());

    // Negotiation body with updated values
    NegotiationCreateDTO updateRequest = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(REQUESTS_ENDPOINT)
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    assertEquals(1, negotiationRepository.count());
  }

  @Test
  @Order(1)
  public void testCreate_Ok_whenProjectIsIncluded() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, true,
        Set.of(testRequest.getId()));
    String requestBody = TestUtils.jsonFromRequest(request);
    long currentRequest = negotiationRepository.count();
    long currentProject = projectRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(REQUESTS_ENDPOINT))
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.queries[0].id", is(testRequest.getId())))
        .andExpect(jsonPath("$.project.id").isString())
        .andExpect(jsonPath("$.project.title", is(TestUtils.PROJECT_TITLE)))
        .andExpect(jsonPath("$.project.description", is(TestUtils.PROJECT_DESCRIPTION)))
        .andExpect(
            jsonPath(
                "$.project.expectedDataGeneration", is(TestUtils.PROJECT_EXPECTED_DATA_GENERATION)))
        .andExpect(jsonPath("$.project.expectedEndDate", is(TestUtils.PROJECT_EXPECTED_END_DATE)))
        .andExpect(jsonPath("$.project.isTestProject", is(TestUtils.PROJECT_IS_TEST_PROJECT)))
        .andReturn();

    assertEquals(negotiationRepository.count(), currentRequest + 1);
    assertEquals(projectRepository.count(), currentProject + 1);
  }

  @Test
  @Order(2)
  public void testCreate_Ok_whenProjectAlreadyExists() throws Exception {
    // Create the project before
    Project projectEntity = modelMapper.map(TestUtils.createProjectRequest(false), Project.class);
    projectRepository.save(projectEntity);

    NegotiationCreateDTO request = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    String requestBody = TestUtils.jsonFromRequest(request);

    long currentRequest = negotiationRepository.count();
    long currentProject = projectRepository.count();

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(
                    URI.create(PROJECTS_ENDPOINT.formatted(projectEntity.getId())))
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.title", is(TITLE)))
        .andExpect(jsonPath("$.description", is(DESCRIPTION)))
        .andExpect(jsonPath("$.queries[0].id", is(testRequest.getId())));

    assertEquals(negotiationRepository.count(), currentRequest + 1);
    assertEquals(projectRepository.count(), currentProject);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(false, false,
        Set.of(testRequest.getId()));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        "%s/1".formatted(REQUESTS_ENDPOINT));
  }

  @Test
  public void testUpdate_BadRequest_whenQueryIsAlreadyAssignedToAnotherRequest() throws Exception {
    // Create the negotiation that has the assigned request
    Negotiation negotiationEntityWithQuery =
        modelMapper.map(TestUtils.createNegotiation(false, false, null), Negotiation.class);
    negotiationRepository.save(negotiationEntityWithQuery);
    Request firstRequest = createQueryEntity();
    firstRequest.setNegotiation(negotiationEntityWithQuery);
    requestRepository.save(firstRequest);

    // Create the negotiation to update
    Negotiation negotiationEntityUpdate =
        modelMapper.map(TestUtils.createNegotiation(false, false, null), Negotiation.class);
    negotiationRepository.save(negotiationEntityUpdate);
    Request secondRequest = createQueryEntity();
    secondRequest.setNegotiation(negotiationEntityUpdate);
    requestRepository.save(secondRequest);

    // Negotiation body with updated values and request already assigned
    NegotiationCreateDTO request =
        TestUtils.createNegotiation(true, false,
            Set.of(firstRequest.getId(), secondRequest.getId()));
    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                    "%s/%s".formatted(REQUESTS_ENDPOINT, negotiationEntityUpdate.getId()))
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  public void testUpdate_Ok_whenChangeTitle() throws Exception {
    // The data source to be updated
    Negotiation negotiationEntity =
        modelMapper.map(
            TestUtils.createNegotiation(false, false, Set.of(testRequest.getId())),
            Negotiation.class);
    negotiationRepository.save(negotiationEntity);

    testRequest.setNegotiation(negotiationEntity);
    requestRepository.save(testRequest);

    // Negotiation body with updated values
    NegotiationCreateDTO request = TestUtils.createNegotiation(true, false,
        Set.of(testRequest.getId()));
    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                    "%s/%s".formatted(REQUESTS_ENDPOINT, negotiationEntity.getId()))
                .header("Authorization", "Bearer %s".formatted(CORRECT_TOKEN_VALUE))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }
}
