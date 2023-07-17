package eu.bbmri.eric.csit.service.negotiator.integration.api.v2;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v2.QueryV2Controller;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryCreateV2DTO;
import eu.bbmri.eric.csit.service.negotiator.integration.api.v3.TestUtils;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class QueryV2ControllerTests {

  private static final String REQUEST_V2_ID = "request-v2";
  private static final String NEGOTIATION_V2_ID = "negotiation-v2";

  private static final String ENDPOINT = "/directory/create_query";
  @Autowired public RequestRepository requestRepository;
  @Autowired private WebApplicationContext context;
  @Autowired private QueryV2Controller controller;
  @Autowired private RequestServiceImpl requestService;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private ModelMapper modelMapper;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  public void testCreate_BadRequest_whenUrlFieldIsMissing() throws Exception {
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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
  @Disabled
  public void testCreate_BadRequest_whenCollectionAndBiobankMismatch() throws Exception {
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
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

    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
    String requestBody = TestUtils.jsonFromRequest(request);
    long previousCount = requestRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", containsString("http://localhost/request")))
        .andExpect(jsonPath("$.redirect_uri", containsString("http://localhost/request")));
    assertEquals(requestRepository.count(), previousCount + 1);
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    QueryCreateV2DTO request = TestUtils.createQueryV2Request();
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  public void testUpdate_CreateWhenRequestIsNotFound() throws Exception {
    QueryCreateV2DTO updateRequest = TestUtils.createQueryV2Request();
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
        .andExpect(header().string("Location", containsString("http://localhost/request")))
        .andExpect(jsonPath("$.redirect_uri", containsString("http://localhost/request")));
  }

  @Test
  @Transactional
  public void testUpdate_Ok_whenChangeQuery() throws Exception {
    QueryCreateV2DTO updateRequest = TestUtils.createQueryV2Request();
    updateRequest.setToken("%s__search__%s".formatted(NEGOTIATION_V2_ID, REQUEST_V2_ID));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    long previousCount = requestRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isAccepted())
        .andExpect(
            header()
                .string(
                    "Location",
                    containsString(
                        "http://localhost/negotiations/%s/requests".formatted(NEGOTIATION_V2_ID))))
        .andExpect(
            jsonPath(
                "$.redirect_uri",
                containsString(
                    "http://localhost/negotiations/%s/requests".formatted(NEGOTIATION_V2_ID))));
    assertEquals(requestRepository.count(), previousCount);
  }

  @Test
  public void testUpdate_Ok_whenAddQueryToARequest() throws Exception {
    QueryCreateV2DTO updateRequest = TestUtils.createQueryV2Request();
    updateRequest.setToken("%s__search__".formatted(NEGOTIATION_V2_ID));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    long previousRequestNumber = requestRepository.findAll().size();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(ENDPOINT)
                .with(httpBasic("directory", "directory"))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isAccepted())
        .andExpect(
            header()
                .string(
                    "Location",
                    containsString(
                        "http://localhost/negotiations/%s/requests".formatted(NEGOTIATION_V2_ID))))
        .andExpect(
            jsonPath(
                "$.redirect_uri",
                containsString(
                    "http://localhost/negotiations/%s/requests".formatted(NEGOTIATION_V2_ID))));
    assertEquals(requestRepository.findAll().size(), previousRequestNumber + 1);
  }
}
