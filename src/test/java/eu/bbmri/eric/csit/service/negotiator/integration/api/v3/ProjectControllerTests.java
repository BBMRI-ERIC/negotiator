package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.ProjectController;
import eu.bbmri.eric.csit.service.negotiator.database.model.Project;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ProjectRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.project.ProjectCreateDTO;
import java.net.URI;
import org.hamcrest.core.Is;
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
import org.springframework.security.test.context.support.WithMockUser;
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
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
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
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  @Order(1)
  @WithMockUser(authorities = "ROLE_RESEARCHER")
  public void testCreate_Ok_whenIsTestProject_isDefault() throws Exception {
    ProjectCreateDTO request = TestUtils.createProjectRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            post(URI.create("/v3/projects"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.payload", Is.is(TestUtils.PROJECT_PAYLOAD)));
    assertEquals(repository.findAll().size(), 1);
  }

  @Test
  public void testGetAll_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testGetAll_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testGetAll_Unauthorized_whenInvalidToken() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), "", ENDPOINT);
  }

  @Test
  @WithMockUser
  public void testGetAll_Forbidden_whenNoPermission() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/v3/projects")).andExpect(status().isForbidden());
  }

  @Test
  public void testGetAll_Forbidden_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  @WithMockUser(authorities = "ROLE_RESEARCHER")
  public void testGetAll_Ok() throws Exception {
    ProjectCreateDTO projectRequest = TestUtils.createProjectRequest(false);
    Project entity = modelMapper.map(projectRequest, Project.class);
    entity = repository.save(entity);

    mockMvc
        .perform(MockMvcRequestBuilders.get(ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].id", is(entity.getId())))
        .andExpect(jsonPath("$[0].payload", is(TestUtils.PROJECT_PAYLOAD)));
  }

  @Test
  public void testGetById_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        anonymous(),
        String.format("%s/1", ENDPOINT));
  }

  @Test
  public void testGetById_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        String.format("%s/1", ENDPOINT));
  }

  @Test
  public void testGetById_Unauthorized_whenInvalidToken() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        "",
        String.format("%s/1", ENDPOINT));
  }

  @Test
  public void testGetById_Forbidden_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isForbidden(),
        httpBasic("directory", "directory"),
        String.format("%s/1", ENDPOINT));
  }

  @Test
  @WithMockUser(authorities = "ROLE_RESEARCHER")
  public void testGetById_NotFound_WhenTheIdIsNotPresent() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(String.format("%s/%s", ENDPOINT, "1")))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(authorities = "ROLE_RESEARCHER")
  public void testGetById_Ok() throws Exception {
    ProjectCreateDTO projectRequest = TestUtils.createProjectRequest(false);
    Project entity = modelMapper.map(projectRequest, Project.class);
    entity = repository.save(entity);

    mockMvc
        .perform(MockMvcRequestBuilders.get(String.format("%s/%s", ENDPOINT, entity.getId())))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(entity.getId())))
        .andExpect(jsonPath("$.payload", is(TestUtils.PROJECT_PAYLOAD)));
  }
}
