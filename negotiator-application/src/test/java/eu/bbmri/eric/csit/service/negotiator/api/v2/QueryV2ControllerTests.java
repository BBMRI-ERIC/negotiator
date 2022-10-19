package eu.bbmri.eric.csit.service.negotiator.api.v2;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v2.QueryV2Controller;
import eu.bbmri.eric.csit.service.negotiator.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryV2Request;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.model.Request;
import eu.bbmri.eric.csit.service.negotiator.repository.QueryRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.RequestRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestMethodOrder(OrderAnnotation.class)
public class QueryV2ControllerTests {
  private static final String ENDPOINT = "/api/directory/create_query";
  @Autowired public QueryRepository queryRepository;
  @Autowired private WebApplicationContext context;
  @Autowired private QueryV2Controller controller;
  @Autowired private QueryService queryService;
  @Autowired private RequestRepository requestRepository;
  @Autowired private ModelMapper modelMapper;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    queryRepository.deleteAll();
  }

  @Test
  public void testCreate_BadRequest_whenUrlFieldIsMissing() throws Exception {
    QueryV2Request request = TestUtils.createQueryV2Request(false);
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
    QueryV2Request request = TestUtils.createQueryV2Request(false);
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
  public void testCreate_BadRequest_whenCollectionFieldIsMissing() throws Exception {
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    request.setCollections(null);
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
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    request.setCollections(Set.of());
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
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    Optional<CollectionV2DTO> collection = request.getCollections().stream().findFirst();
    assert collection.isPresent();
    collection.get().setCollectionId("collection_unknown");
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
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    Optional<CollectionV2DTO> biobank = request.getCollections().stream().findFirst();
    assert biobank.isPresent();
    biobank.get().setBiobankId("wrong_biobank");
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
    QueryV2Request request = TestUtils.createQueryV2Request(false);
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
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(
            header().string("Location", containsString("http://localhost/gui/request/jsonQuery=")))
        .andExpect(
            jsonPath("$.redirect_uri", containsString("http://localhost/gui/request/jsonQuery=")));
    assertEquals(queryRepository.findAll().size(), 1);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    QueryV2Request request = TestUtils.createQueryV2Request(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isForbidden(),
        httpBasic("researcher", "researcher"),
        ENDPOINT);
  }

  @Test
  public void testUpdate_CreateWhenRequestIsNotFound() throws Exception {
    QueryV2Request updateRequest = TestUtils.createQueryV2Request(false);
    updateRequest.setToken("-1__search__-1");
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(
            header().string("Location", containsString("http://localhost/gui/request/jsonQuery=")))
        .andExpect(
            jsonPath("$.redirect_uri", containsString("http://localhost/gui/request/jsonQuery=")));
  }

  @Test
  @Order(3)
  @Transactional
  public void testUpdate_Ok_whenChangeQuery() throws Exception {
    Query q = queryService.create(TestUtils.createQueryRequest(false));
    // The data source to be updated
    Request requestEntity =
        modelMapper.map(TestUtils.createRequest(false, false, Set.of(q.getId())), Request.class);
    q.setRequest(requestEntity);
    requestRepository.save(requestEntity);

    QueryV2Request updateRequest = TestUtils.createQueryV2Request(false);
    updateRequest.setToken("%s__search__%s".formatted(requestEntity.getToken(), q.getToken()));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isAccepted())
        .andExpect(
            header().string("Location", containsString(
                    "http://localhost/gui/request/queryId=%sjsonQuery="
                        .formatted(requestEntity.getId()))))
        .andExpect(
            jsonPath(
                "$.redirect_uri",
                containsString(
                    "http://localhost/gui/request/queryId=%sjsonQuery="
                        .formatted(requestEntity.getId()))));
    assertEquals(queryRepository.findAll().size(), 1);
  }

  @Test
  @Order(3)
  @Transactional
  public void testUpdate_Ok_whenAddQueryToARequest() throws Exception {
    Query q = queryService.create(TestUtils.createQueryRequest(false));
    // The data source to be updated
    Request requestEntity =
        modelMapper.map(TestUtils.createRequest(false, false, Set.of(q.getId())), Request.class);
    q.setRequest(requestEntity);
    requestRepository.save(requestEntity);

    QueryV2Request updateRequest = TestUtils.createQueryV2Request(false);
    updateRequest.setToken("%s__search__".formatted(requestEntity.getToken()));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isAccepted())
        .andExpect(
            header().string("Location", containsString(
                "http://localhost/gui/request/queryId=%sjsonQuery="
                    .formatted(requestEntity.getId()))))
        .andExpect(
            jsonPath(
                "$.redirect_uri",
                containsString(
                    "http://localhost/gui/request/queryId=%sjsonQuery="
                        .formatted(requestEntity.getId()))));
    assertEquals(queryRepository.findAll().size(), 2);
  }
}
