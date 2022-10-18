package eu.bbmri.eric.csit.service.negotiator.api.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.QueryController;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.QueryCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Query;
import eu.bbmri.eric.csit.service.negotiator.database.repository.QueryRepository;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import java.util.Optional;
import java.util.Set;
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
public class QueryControllerTests {
  private static final String ENDPOINT = "/v3/queries";
  @Autowired public QueryService service;
  @Autowired public QueryRepository repository;
  @Autowired private WebApplicationContext context;
  @Autowired private QueryController controller;
  @Autowired private QueryService queryService;
  @Autowired private ModelMapper modelMapper;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    repository.deleteAll();
  }

  @Test
  public void testCreate_BadRequest_whenUrlFieldIsMissing() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    request.setUrl(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenUrlHumanReadableFieldIsMissing() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    request.setHumanReadable(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenResourcesFieldIsMissing() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    request.setResources(null);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenResourcesFieldIsEmpty() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    request.setResources(Set.of());
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenCollectionNotFound() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    Optional<ResourceDTO> biobank = request.getResources().stream().findFirst();
    assert biobank.isPresent();
    Optional<ResourceDTO> collection = biobank.get().getChildren().stream().findFirst();
    assert collection.isPresent();
    collection.get().setId("collection_unknown");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenCollectionAndBiobankMismatch() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    Optional<ResourceDTO> biobank = request.getResources().stream().findFirst();
    assert biobank.isPresent();
    biobank.get().setId("wrong_biobank");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  public void testCreate_BadRequest_whenDataSourceNotFound() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    request.setUrl("http://wrong_data_source");
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isBadRequest(),
        httpBasic("directory", "directory"),
        ENDPOINT);
  }

  @Test
  @Order(1)
  public void testCreate_Ok() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/gui/request")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.findAll().size(), 1);
  }

  @Test
  @Order(2)
  public void testGetAll_Ok() throws Exception {
    queryService.create(TestUtils.createQueryRequest(false));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is(1)))
        .andExpect(jsonPath("$[0].id").isString())
        .andExpect(jsonPath("$[0].url", is("http://datasource.dev")))
        .andExpect(jsonPath("$[0].redirectUrl", containsString("http://localhost/gui/request")))
        .andExpect(jsonPath("$[0].resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$[0].resources[0].type", is("biobank")))
        .andExpect(jsonPath("$[0].resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$[0].resources[0].children[0].type", is("collection")));
    assertEquals(repository.findAll().size(), 1);
  }

  @Test
  @Order(2)
  public void testGetById_Ok() throws Exception {
    Query q = queryService.create(TestUtils.createQueryRequest(false));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/%s".formatted(ENDPOINT, q.getId()))
                .with(httpBasic("directory", "directory"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/gui/request")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.findAll().size(), 1);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(ENDPOINT));
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        "%s/1".formatted(ENDPOINT));
  }

  @Test
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    QueryCreateDTO request = TestUtils.createQueryRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isForbidden(),
        httpBasic("researcher", "researcher"),
        "%s/1".formatted(ENDPOINT));
  }


  @Test
  public void testUpdate_NotFound() throws Exception {
    QueryCreateDTO updateRequest = TestUtils.createQueryRequest(true);
    String requestBody = TestUtils.jsonFromRequest(updateRequest);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("%s/-1".formatted(ENDPOINT))
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNotFound());
  }

  @Test
  @Order(3)
  public void testUpdate_Ok() throws Exception {
    Query q = queryService.create(TestUtils.createQueryRequest(false));

    QueryCreateDTO updateRequest = TestUtils.createQueryRequest(true);
    String requestBody = TestUtils.jsonFromRequest(updateRequest);

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("%s/%s".formatted(ENDPOINT, q.getId()))
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/gui/request")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:2")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:2:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.findAll().size(), 1);
  }
}
