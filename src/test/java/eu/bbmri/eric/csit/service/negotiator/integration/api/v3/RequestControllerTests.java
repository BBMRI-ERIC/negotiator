package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.RequestController;
import eu.bbmri.eric.csit.service.negotiator.service.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.service.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.service.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.service.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationStateService;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
public class RequestControllerTests {

  private static final Long CREATOR_ID = 104L;
  private static final String ENDPOINT = "/v3/requests";
  private static final String NEGOTIATION_1 = "negotiation-1";
  private static final String UNASSIGNED_REQUEST_ID = "request-unassigned";

  @Autowired
  public RequestServiceImpl service;
  @Autowired
  public RequestRepository repository;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private RequestController controller;
  @Autowired
  private RequestServiceImpl requestService;
  @Autowired
  private NegotiationServiceImpl negotiationService;
  @Autowired
  private NegotiationStateService negotiationStateService;
  @Autowired
  private ModelMapper modelMapper;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testCreate_BadRequest_whenUrlFieldIsMissing() throws Exception {
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
  public void testCreate_Ok() throws Exception {
    RequestCreateDTO request = TestUtils.createRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);
    long previousCount = repository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/requests/")))
        .andExpect(jsonPath("$.negotiationId").doesNotExist())
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.count(), previousCount + 1);
  }

  @Test
  public void testGetAll_Ok_whenNoNegotiationIsAssigned() throws Exception {
    requestService.create(TestUtils.createRequest(false));
    String unassignedRequestSelector = "$[?(@.id == '%s')]".formatted(UNASSIGNED_REQUEST_ID);
    long previousCount = repository.count();

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is((int) previousCount)))
        .andExpect(jsonPath(unassignedRequestSelector).exists())
        .andExpect(jsonPath(String.format("%s.negotiationId", unassignedRequestSelector)).doesNotExist());
    assertEquals(repository.count(), previousCount);
  }

  @Test
  public void testGetAll_Ok_whenNegotiationIsAssigned() throws Exception {
    long previousCount = repository.count();

    mockMvc
        .perform(
            MockMvcRequestBuilders.get(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()", is((int) previousCount)))
        .andExpect(jsonPath("$[0].id").isString())
        .andExpect(jsonPath("$[0].url", is("http://datasource.dev")))
        .andExpect(jsonPath("$[0].redirectUrl", containsString("http://localhost/request")))
        .andExpect(jsonPath("$[0].negotiationId", is(NEGOTIATION_1)))
        .andExpect(jsonPath("$[0].resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$[0].resources[0].type", is("biobank")))
        .andExpect(jsonPath("$[0].resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$[0].resources[0].children[0].type", is("collection")));
    assertEquals(repository.count(), previousCount);
  }

  @Test
  public void testGetById_Ok_whenNoNegotiationIsAssigned() throws Exception {
    RequestDTO r = requestService.create(TestUtils.createRequest(false));
    long previousCount = repository.count();

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/%s".formatted(ENDPOINT, r.getId()))
                .with(httpBasic("directory", "directory"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/request")))
        .andExpect(jsonPath("$.negotiationId").doesNotExist())
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.count(), previousCount);
  }

  @Test
  public void testGetById_Ok_whenNegotiationIsAssigned() throws Exception {
    RequestDTO r = requestService.create(TestUtils.createRequest(false));
    NegotiationDTO n = negotiationService.create(
        TestUtils.createNegotiation(Collections.singleton(r.getId())), CREATOR_ID);

    long previousCount = repository.count();

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/%s".formatted(ENDPOINT, r.getId()))
                .with(httpBasic("directory", "directory"))
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/request")))
        .andExpect(jsonPath("$.negotiationId", is(n.getId())))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:1")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:1:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.count(), previousCount);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO request = TestUtils.createRequest(false);
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
    RequestCreateDTO updateRequest = TestUtils.createRequest(true);
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
  public void testUpdate_Ok() throws Exception {
    RequestDTO r = requestService.create(TestUtils.createRequest(false));

    RequestCreateDTO updateRequest = TestUtils.createRequest(true);
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    long previousCount = repository.count();

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("%s/%s".formatted(ENDPOINT, r.getId()))
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.url", is("http://datasource.dev")))
        .andExpect(jsonPath("$.redirectUrl", containsString("http://localhost/request")))
        .andExpect(jsonPath("$.resources[0].id", is("biobank:2")))
        .andExpect(jsonPath("$.resources[0].type", is("biobank")))
        .andExpect(jsonPath("$.resources[0].children[0].id", is("biobank:2:collection:1")))
        .andExpect(jsonPath("$.resources[0].children[0].type", is("collection")));
    assertEquals(repository.count(), previousCount);
  }
}
