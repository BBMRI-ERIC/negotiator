package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.discovery.DiscoverServiceController;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery.dto.DiscoveryServiceCreateDTO;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoverySyncJobServiceUpdateDTO;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class DiscoveryServiceControllerTests {

  private static final String ENDPOINT = "/v3/discovery-services";
  private MockMvc mockMvc;
  @Autowired private WebApplicationContext context;
  @Autowired private DiscoverServiceController controller;
  @Autowired private DiscoveryServiceRepository repository;
  @Autowired private ModelMapper modelMapper;
  @Autowired DiscoveryServiceSynchronizationJobRepository jobRepository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testCreate_BadRequest_whenName_IsMissing() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    request.setName(null);
    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/discovery-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testCreate_BadRequest_whenUrl_IsMissing() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    request.setUrl(null);
    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/discovery-services")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithUserDetails("admin")
  @Transactional
  public void testCreated_whenRequest_IsCorrect() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    String requestBody = TestUtils.jsonFromRequest(request);

    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/v3/discovery-services")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.name", Is.is(TestUtils.DISCOVERY_SERVICE_NAME)))
            .andExpect(jsonPath("$.url", Is.is(TestUtils.DISCOVERY_SERVICE_URL)))
            .andReturn();
    Integer discoveryServiceId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
    Optional<DiscoveryService> discoveryService = repository.findById((long) discoveryServiceId);
    assert discoveryService.isPresent();
    assertEquals(discoveryService.get().getCreatedBy().getName(), "admin");

    assertEquals(repository.findAll().size(), 2);
    repository.deleteById(2L);
  }

  @Test
  public void testCreate_Unauthorized_whenNoAuth() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.POST, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testCreate_Unauthorized_whenWrongAuth() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.POST,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  @WithMockUser
  public void testCreate_Forbidden_whenNoPermission() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    mockMvc
        .perform(MockMvcRequestBuilders.post("/v3/discovery-services"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  public void testUpdate_whenIsCorrect() throws Exception {
    // The data source to be updated
    DiscoveryService discoveryServiceEntity =
        modelMapper.map(TestUtils.createDiscoveryServiceRequest(false), DiscoveryService.class);
    repository.save(discoveryServiceEntity);

    // Negotiation body with updated values
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(true);

    String requestBody = TestUtils.jsonFromRequest(request);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put(
                    "/v3/discovery-services/%s".formatted(discoveryServiceEntity.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNoContent())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON));

    Optional<DiscoveryService> updateDiscoveryService =
        repository.findById(discoveryServiceEntity.getId());
    assert updateDiscoveryService.isPresent();
    assertEquals(updateDiscoveryService.get(), modelMapper.map(request, DiscoveryService.class));

    repository.deleteById(discoveryServiceEntity.getId());
  }

  @Test
  public void testUpdate_Unauthorized_whenNoAuth() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc, HttpMethod.PUT, request, status().isUnauthorized(), anonymous(), ENDPOINT);
  }

  @Test
  public void testUpdate_Unauthorized_whenWrongAuth() throws Exception {
    DiscoveryServiceCreateDTO request = TestUtils.createDiscoveryServiceRequest(false);
    TestUtils.checkErrorResponse(
        mockMvc,
        HttpMethod.PUT,
        request,
        status().isUnauthorized(),
        httpBasic("admin", "wrong_pass"),
        ENDPOINT);
  }

  @Test
  @WithUserDetails("researcher")
  public void testUpdate_Forbidden_whenNoPermission() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.put("/v3/discovery-services"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("researcher")
  public void test_getAllDiscoveryServices() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(ENDPOINT).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  @WithUserDetails("researcher")
  public void retrieveDiscoveryService_success() throws Exception {
    long discoveryServiceId = 1L;
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/discovery-services/{id}", discoveryServiceId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(discoveryServiceId));
  }

  @Test
  @WithUserDetails("researcher")
  public void retrieveDiscoveryService_notFound() throws Exception {
    long nonexistentId = 999L;
    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/v3/discovery-services/{id}", nonexistentId)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  public void test_CreateDiscoveryServiceSynchronizationJJob_ok() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/discovery-services/1/sync-jobs")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isCreated());
  }

  @Test
  @WithUserDetails("researcher")
  public void test_CreateDiscoveryServiceSynchronizationJob_Unauthorized() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/discovery-services/1/sync-jobs")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithUserDetails("admin")
  public void test_CreateDiscoveryServiceSynchronizationJJob_UnknownService() throws Exception {
    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v3/discovery-services/999/sync-job")
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithUserDetails("admin")
  public void test_UpdateSynchronizationJob_ok() throws Exception {
    DiscoveryServiceSynchronizationJob job =
        DiscoveryServiceSynchronizationJob.builder()
            .status(DiscoveryServiceSyncronizationJobStatus.SUBMITTED)
            .creationDate(LocalDateTime.now())
            .modifiedDate(LocalDateTime.now())
            .service(repository.findById(1L).get())
            .build();
    jobRepository.save(job);
    String jobId = jobRepository.findAll().get(0).getId();
    DiscoverySyncJobServiceUpdateDTO jobUpdateDTO =
        DiscoverySyncJobServiceUpdateDTO.builder()
            .jobStatus(DiscoveryServiceSyncronizationJobStatus.COMPLETED)
            .build();
    String requestBody = TestUtils.jsonFromRequest(jobUpdateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch("/v3/discovery-services/1/sync-jobs/" + jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk());

    DiscoveryServiceSynchronizationJob updatedJob = jobRepository.findById(jobId).get();
    assertEquals(updatedJob.getStatus(), DiscoveryServiceSyncronizationJobStatus.COMPLETED);
  }
}
