package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.api.controller.v3.NegotiationController;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiationControllerTests {

  // Request alrady present in data-h2. It is already assigned to a request
  private static final String REQUEST_1_ID = "request-1";
  private static final String REQUEST_2_ID = "request-2";
  private static final String REQUEST_3_ID = "request-3";
  private static final String NEGOTIATION_1_ID = "negotiation-1";
  private static final String NEGOTIATION_2_ID = "negotiation-2";
  private static final String NEGOTIATION_3_ID = "negotiation-3";

  private static final String NEGOTIATIONS_URL = "/v3/negotiations";
  private static final String CORRECT_TOKEN_VALUE = "researcher";
  private static final String FORBIDDEN_TOKEN_VALUE = "unknown";
  private static final String UNAUTHORIZED_TOKEN_VALUE = "unauthorized";

  @Autowired
  private WebApplicationContext context;
  @Autowired
  private NegotiationController negotiationController;
  @Autowired
  private NegotiationRepository negotiationRepository;
  @Autowired
  private RequestRepository requestRepository;
  @Autowired
  private ModelMapper modelMapper;
  @Autowired
  private RequestServiceImpl requestService;

  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  private RequestDTO createRequestEntity() {
    RequestCreateDTO queryRequest = TestUtils.createRequest(false);
    return requestService.create(queryRequest);
  }

  @Test
  public void testGetAll_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), anonymous(), NEGOTIATIONS_URL);
  }

  @Test
  public void testGetAll_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        NEGOTIATIONS_URL);
  }

  @Test
  public void testGetAll_Unauthorized_whenInvalidToken() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.GET, "", status().isUnauthorized(), "", NEGOTIATIONS_URL);
  }

  @Test
  @WithMockUser
  public void testGetAll_Ok() throws Exception {
    int numberOfNegotiations = (int) negotiationRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(NEGOTIATIONS_URL))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()", is(numberOfNegotiations)))
        .andExpect(jsonPath("$[0].id", is(NEGOTIATION_1_ID)))
        .andExpect(jsonPath("$[1].id", is(NEGOTIATION_2_ID)));
  }

  @Test
  public void testGetById_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        anonymous(),
        "%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID));
  }

  @Test
  public void testGetById_Unauthorized_whenBasicAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.GET,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        "%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID));
  }

  @Test
  @WithMockUser
  public void testGetById_NotFound_whenWrongId() throws Exception {
    mockMvc
            .perform(
                    MockMvcRequestBuilders.get("%s/-1".formatted(NEGOTIATIONS_URL)))
            .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser
  public void testGetById_Ok_whenCorrectId() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID)))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(NEGOTIATION_1_ID)));
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        "",
        status().isUnauthorized(),
        anonymous(),
        NEGOTIATIONS_URL);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        "",
        status().isUnauthorized(),
        httpBasic("researcher", "wrong_pass"),
        NEGOTIATIONS_URL);
  }

  @Test
  public void testCreate_Unauthorized() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post(NEGOTIATIONS_URL)).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser
  public void testCreate_BadRequest_whenRequests_IsMissing() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_2_ID));
    request.setRequests(null);
    mockMvc.perform(MockMvcRequestBuilders
                    .post(NEGOTIATIONS_URL)
                    .contentType(MediaType.APPLICATION_JSON).content(TestUtils.jsonFromRequest(request)))
            .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser
  public void testCreate_BadRequest_whenRequests_IsEmpty() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Collections.emptySet());
    mockMvc.perform(MockMvcRequestBuilders
            .post(NEGOTIATIONS_URL)
            .contentType(MediaType.APPLICATION_JSON).content(TestUtils.jsonFromRequest(request)))
            .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("researcher")
  public void testCreate_BadRequest_whenSomeRequests_IsNotFound() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of("unknown"));
    mockMvc.perform(MockMvcRequestBuilders
                    .post(NEGOTIATIONS_URL)
                    .contentType(MediaType.APPLICATION_JSON).content(TestUtils.jsonFromRequest(request)))
            .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("researcher")
  public void testCreate_BadRequest_whenRequest_IsAlreadyAssignedToAnotherRequest()
      throws Exception {
    // It tries to create a request by assigning the already assigned REQUEST_1
    NegotiationCreateDTO negotiationBody = TestUtils.createNegotiation(Set.of(REQUEST_1_ID));
    String requestBody = TestUtils.jsonFromRequest(negotiationBody);
    long previousRequestCount = negotiationRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(NEGOTIATIONS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andDo(print())
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    assertEquals(negotiationRepository.count(), previousRequestCount);
  }

  @Test
  @WithUserDetails("researcher")
  public void testCreate_Ok() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_2_ID));
    String requestBody = TestUtils.jsonFromRequest(request);
    long previousRequestCount = negotiationRepository.count();
    mockMvc
        .perform(
            MockMvcRequestBuilders.post(URI.create(NEGOTIATIONS_URL))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.payload.project.title",
            is("Title")))
        .andExpect(jsonPath("$.payload.samples.num-of-subjects",
            is(10)))
        .andExpect(jsonPath("$.payload.ethics-vote.ethics-vote",
            is("My ethic vote")));

    assertEquals(negotiationRepository.count(), previousRequestCount + 1);
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    NegotiationCreateDTO negotiationBody = TestUtils.createNegotiation(Set.of(REQUEST_2_ID));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        negotiationBody,
        status().isUnauthorized(),
        anonymous(),
        "%s/1".formatted(NEGOTIATIONS_URL));
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_2_ID));
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        "%s/1".formatted(NEGOTIATIONS_URL));
  }

  @Test
  @WithMockUser
  public void testUpdate_BadRequest_whenRequestIsAlreadyAssignedToAnotherNegotiation()
      throws Exception {
    //It tries to update the known NEGOTIATION_2 by assigning the request of NEGOTIATION_1
    NegotiationCreateDTO updateRequest = TestUtils.createNegotiation(
        Set.of("request-1", "request-2"));
    String requestBody = TestUtils.jsonFromRequest(updateRequest);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                    "%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_2_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @WithMockUser
  public void testUpdate_Ok_whenChangePayload() throws Exception {
    // Tries to updated negotiation
    // Negotiation body with updated values
    NegotiationCreateDTO request = TestUtils.createNegotiation(Set.of(REQUEST_1_ID));
    String requestBody = TestUtils.jsonFromRequest(request);
    requestBody = requestBody.replace("Title", "New Title");

    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                    "%s/%s".formatted(NEGOTIATIONS_URL, NEGOTIATION_1_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));
  }

  @Test
  @WithMockUser
  public void testNoNegotiationsAreReturned() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("%s?userRole=RESEARCHER".formatted(NEGOTIATIONS_URL)))
        .andExpect(status().isOk()).andExpect(content().json("[]"));
  }
}
