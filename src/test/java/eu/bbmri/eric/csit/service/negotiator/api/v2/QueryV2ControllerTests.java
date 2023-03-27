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
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryCreateV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
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

  private static final String ENDPOINT = "/directory/create_query";
  @Autowired
  public RequestRepository requestRepository;
  @Autowired
  private WebApplicationContext context;
  @Autowired
  private QueryV2Controller controller;
  @Autowired
  private RequestService requestService;
  @Autowired
  private NegotiationRepository negotiationRepository;
  @Autowired
  private ModelMapper modelMapper;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    requestRepository.deleteAll();
  }

  @Test
  public void testCreate_BadRequest_whenUrlFieldIsMissing() throws Exception {
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(
            header().string("Location", containsString("http://localhost/request")))
        .andExpect(
            jsonPath("$.redirect_uri", containsString("http://localhost/request")));
    assertEquals(requestRepository.findAll().size(), 1);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request(false);
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
    QueryCreateV2DTO updateRequest = TestUtils.createQueryV2Request(false);
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
            header().string("Location", containsString("http://localhost/request")))
        .andExpect(
            jsonPath("$.redirect_uri", containsString("http://localhost/request")));
  }

  @Test
  @Order(3)
  @Transactional
  public void testUpdate_Ok_whenChangeQuery() throws Exception {
    Request q = requestService.create(TestUtils.createRequest(false));
    Negotiation negotiationEntity =
        modelMapper.map(TestUtils.createNegotiation(false, false, Set.of(q.getId())),
            Negotiation.class);
    q.setNegotiation(negotiationEntity);
    negotiationRepository.save(negotiationEntity);

    QueryCreateV2DTO updateRequest = TestUtils.createQueryV2Request(false);
    updateRequest.setToken("%s__search__%s".formatted(negotiationEntity.getId(), q.getId()));
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
                "http://localhost/negotiations/%s/requests"
                    .formatted(negotiationEntity.getId()))))
        .andExpect(
            jsonPath(
                "$.redirect_uri",
                containsString(
                    "http://localhost/negotiations/%s/requests"
                        .formatted(negotiationEntity.getId()))));
    assertEquals(requestRepository.findAll().size(), 1);
  }

  @Test
  @Order(3)
  public void testUpdate_Ok_whenAddQueryToARequest() throws Exception {
    Request q = requestService.create(TestUtils.createRequest(false));
    // The data source to be updated
    Negotiation negotiationEntity =
        modelMapper.map(TestUtils.createNegotiation(false, false, Set.of(q.getId())),
            Negotiation.class);
    q.setNegotiation(negotiationEntity);
    negotiationRepository.save(negotiationEntity);

    QueryCreateV2DTO updateRequest = TestUtils.createQueryV2Request(false);
    updateRequest.setToken("%s__search__".formatted(negotiationEntity.getId()));
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
                "http://localhost/negotiations/%s/requests"
                    .formatted(negotiationEntity.getId()))))
        .andExpect(
            jsonPath(
                "$.redirect_uri",
                containsString(
                    "http://localhost/negotiations/%s/requests"
                        .formatted(negotiationEntity.getId()))));
    assertEquals(requestRepository.findAll().size(), 2);
  }
}
