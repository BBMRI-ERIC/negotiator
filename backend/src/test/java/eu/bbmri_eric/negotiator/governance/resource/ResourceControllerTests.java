package eu.bbmri_eric.negotiator.governance.resource;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.common.FilterDTO;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceFilterDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceUpdateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementService;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmission;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import eu.bbmri_eric.negotiator.util.WithMockNegotiatorUser;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@IntegrationTest(loadTestData = true)
public class ResourceControllerTests {

  private static final String RESOURCE_ENDPOINT = "/v3/resources/%s";

  private static final String RESOURCES_ENDPOINT = "/v3/resources";

  @Autowired private WebApplicationContext context;
  @Autowired private ResourceRepository repository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private AccessFormRepository accessFormRepository;
  private MockMvc mockMvc;
  @Autowired private NegotiationRepository negotiationRepository;
  @Autowired private InformationRequirementService informationRequirementService;
  @Autowired private InformationSubmissionRepository informationSubmissionRepository;
  @Autowired private InformationRequirementRepository informationRequirementRepository;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  @WithMockUser("researcher")
  void getResourceById_validId_ok() throws Exception {
    Resource resource = repository.findById(4L).get();
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_ENDPOINT.formatted(4)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(resource.getId().intValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceId", is(resource.getSourceId())))
    .andExpect(jsonPath("$.organization.name", is(resource.getOrganization().getName())));
  }

  @Test
  @Transactional
  @WithMockUser("researcher")
  @Disabled
  void getAll_10kResourcesInDb_ok() throws Exception {
    DiscoveryService discoveryService =
        discoveryServiceRepository.save(DiscoveryService.builder().url("").name("").build());
    for (int i = 1000; i < 11000; i++) {
      Organization organization =
          organizationRepository.save(
              Organization.builder().name("test").externalId("biobank:%s".formatted(i)).build());
      repository.save(
          Resource.builder()
              .organization(organization)
              .discoveryService(discoveryService)
              .sourceId("collection:%s".formatted(i))
              .name("test")
              .build());
    }
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_ENDPOINT))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(10007)));
  }

  @Test
  @WithMockUser
  void getAllResourcesForNegotiation_nonExistingNegotiation_404() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get("/v3/negotiations/notExistent/resources"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockNegotiatorUser(id = 108L)
  void getAllResourcesForNegotiation_isAuthor_ok() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().stream().findFirst().get();
    assertEquals(108L, negotiation.getCreatedBy().getId());
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].sourceId").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links").isMap());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L)
  void getAllResourcesForNegotiation_isRepresentative_ok() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().stream().findFirst().get();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].currentState").isString())
        .andExpect(jsonPath("$._embedded.resources[0].sourceId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links").isMap());
  }

  @Test
  @WithMockNegotiatorUser(id = 105L)
  void getAllResourcesForNegotiation_notInvolved_403() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().stream().findFirst().get();
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockNegotiatorUser(id = 109L, authorities = "ROLE_ADMIN")
  @Transactional
  void getAllResources_approvedNegotiation_resourceContainsLifecycleLinks() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().stream().findFirst().get();
    negotiation.setStateForResource(
        negotiation.getResources().iterator().next().getSourceId(),
        NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    negotiationRepository.save(negotiation);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].currentState").isString())
        .andExpect(jsonPath("$._embedded.resources[0].sourceId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.uri").isString())
        .andExpect(
            jsonPath("$._embedded.resources[0]._links.MARK_AS_CHECKING_AVAILABILITY").isNotEmpty());
  }

  @Test
  @Transactional
  @WithMockNegotiatorUser(authorities = "ROLE_ADMIN", id = 109L)
  void getAllResources_approvedNegotiation_resourceContainsAllLinks() throws Exception {
    Negotiation negotiation = negotiationRepository.findAll().stream().findFirst().get();
    Resource resource = negotiation.getResources().iterator().next();
    negotiation.setStateForResource(
        resource.getSourceId(), NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    negotiationRepository.save(negotiation);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].currentState").isString())
        .andExpect(jsonPath("$._embedded.resources[0].sourceId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links.requirement-1").doesNotExist())
        .andExpect(
            jsonPath("$._embedded.resources[0]._links.MARK_AS_CHECKING_AVAILABILITY").isNotEmpty());
    Long requirementId =
        informationRequirementService
            .createInformationRequirement(
                new InformationRequirementCreateDTO(
                    1L, NegotiationResourceEvent.MARK_AS_CHECKING_AVAILABILITY))
            .getId();
    String payload =
        """
                            {
                           "sample-type": "DNA",
                           "num-of-subjects": 10,
                           "num-of-samples": 20,
                           "volume-per-sample": 5
                        }
                        """;
    ObjectMapper mapper = new ObjectMapper();
    JsonNode jsonPayload = mapper.readTree(payload);
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].currentState").isString())
        .andExpect(jsonPath("$._embedded.resources[0].sourceId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links.requirement-1").isNotEmpty())
        .andExpect(
            jsonPath("$._embedded.resources[0]._links.MARK_AS_CHECKING_AVAILABILITY").isNotEmpty());

    InformationRequirement informationRequirement =
        informationRequirementRepository.findById(requirementId).get();
    informationSubmissionRepository.save(
        new InformationSubmission(
            informationRequirement, resource, negotiation, jsonPayload.toString()));
    mockMvc
        .perform(
            MockMvcRequestBuilders.get(
                "/v3/negotiations/%s/resources".formatted(negotiation.getId())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0].id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].currentState").isString())
        .andExpect(jsonPath("$._embedded.resources[0].sourceId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.name").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.description").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.contactEmail").isString())
        .andExpect(jsonPath("$._embedded.resources[0].organization.uri").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links.submission-1").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0]._links.requirement-1").doesNotExist())
        .andExpect(
            jsonPath("$._embedded.resources[0]._links.MARK_AS_CHECKING_AVAILABILITY").isNotEmpty());
  }

  @Test
  @WithMockUser
  void getResources_filterByName_ok() throws Exception {
    ResourceFilterDTO filters = new ResourceFilterDTO();
    filters.setName("test");
    int count = repository.findAll(ResourceSpecificationBuilder.build(filters)).size();
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_ENDPOINT + "?name=test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(count)));
  }

  @Test
  @WithMockUser
  void getResources_pageSize1_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_ENDPOINT + "?name=test&size=1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalPages", is(7)));
  }

  @Test
  @WithUserDetails("admin")
  void addResource_batchOfResources_ok() throws Exception {
    Organization org1 =
        Organization.builder()
            .name("Organization 3")
            .description("Organization 3")
            .externalId("test_organization_3")
            .build();
    Organization org2 =
        Organization.builder()
            .name("Organization 4")
            .description("Organization 4")
            .externalId("test_organization_4")
            .build();
    organizationRepository.save(org1);
    organizationRepository.save(org2);
    Long org1Id = organizationRepository.findByExternalId("test_organization_3").get().getId();
    Long org2Id = organizationRepository.findByExternalId("test_organization_4").get().getId();

    ResourceCreateDTO resourceDTO1 =
        ResourceCreateDTO.builder()
            .name("Resource 1")
            .description("Resource 1")
            .contactEmail("resource1@test.org")
            .uri("https://resource1.test.org")
            .sourceId("resource_1")
            .organizationId(org1Id)
            .accessFormId(1L)
            .discoveryServiceId(1L)
            .build();
    ResourceCreateDTO resourceDTO2 =
        ResourceCreateDTO.builder()
            .name("Resource 2")
            .description("Resource 2")
            .contactEmail("resource2@test.org")
            .uri("https://resource2.test.org")
            .sourceId("test_resource_2")
            .organizationId(org2Id)
            .accessFormId(1L)
            .discoveryServiceId(1L)
            .build();

    DiscoveryService discoveryService =
        DiscoveryService.builder()
            .name("test_discovery_service")
            .id(1L)
            .url("http://discoveryservice.net")
            .build();
    AccessForm accessForm = new AccessForm("test");
    discoveryServiceRepository.save(discoveryService);
    accessFormRepository.save(accessForm);

    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(resourceDTO1, resourceDTO2));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RESOURCES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.resources[0].name", is(resourceDTO1.getName())))
            .andExpect(
                jsonPath("$._embedded.resources[0].sourceId", is(resourceDTO1.getSourceId())))
            .andExpect(
                jsonPath("$._embedded.resources[0].description", is(resourceDTO1.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.resources[0].contactEmail", is(resourceDTO1.getContactEmail())))
            .andExpect(jsonPath("$._embedded.resources[0].uri", is(resourceDTO1.getUri())))
            .andExpect(jsonPath("$._embedded.resources[1].name", is(resourceDTO2.getName())))
            .andExpect(
                jsonPath("$._embedded.resources[1].sourceId", is(resourceDTO2.getSourceId())))
            .andExpect(
                jsonPath("$._embedded.resources[1].description", is(resourceDTO2.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.resources[1].contactEmail", is(resourceDTO2.getContactEmail())))
            .andExpect(jsonPath("$._embedded.resources[1].uri", is(resourceDTO2.getUri())))
            .andReturn();

    String id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.resources[0].sourceId");
    Optional<Resource> resource1 = repository.findBySourceId(id1);
    assert resource1.isPresent();
    assertEquals(resourceDTO1.getName(), resource1.get().getName());
    assertEquals(resourceDTO1.getDescription(), resource1.get().getDescription());
    assertEquals(resourceDTO1.getContactEmail(), resource1.get().getContactEmail());
    assertEquals(resourceDTO1.getUri(), resource1.get().getUri());

    Long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.resources[1].id", Long.class);
    Optional<Resource> resource2 = repository.findById(id2);
    assert resource2.isPresent();
    assertEquals(resourceDTO2.getName(), resource2.get().getName());
    assertEquals(resourceDTO2.getDescription(), resource2.get().getDescription());
    assertEquals(resourceDTO2.getContactEmail(), resource2.get().getContactEmail());
    assertEquals(resourceDTO2.getUri(), resource2.get().getUri());
  }

  @Test
  @WithUserDetails("admin")
  void updateResource_singleResource_ok() throws Exception {
    Organization org1 =
        Organization.builder()
            .name("Organization 1")
            .description("Organization 1")
            .externalId("test_organization_1")
            .build();
    Organization org2 =
        Organization.builder()
            .name("Organization 2")
            .description("Organization 2")
            .externalId("test_organization_2")
            .build();
    organizationRepository.save(org1);
    organizationRepository.save(org2);
    Long org1Id = organizationRepository.findByExternalId("test_organization_1").get().getId();
    Long org2Id = organizationRepository.findByExternalId("test_organization_2").get().getId();

    ResourceCreateDTO resourceDTO1 =
        ResourceCreateDTO.builder()
            .name("Resource 3")
            .description("Resource 3")
            .contactEmail("res3@test.org")
            .uri("http://res3.test.org")
            .sourceId("resource_3")
            .organizationId(org1Id)
            .accessFormId(1L)
            .discoveryServiceId(1L)
            .build();
    ResourceCreateDTO resourceDTO2 =
        ResourceCreateDTO.builder()
            .name("Resource 4")
            .description("Resource 4")
            .contactEmail("res4@test.org")
            .uri("http://res4.test.org")
            .sourceId("resource_4")
            .organizationId(org2Id)
            .accessFormId(1L)
            .discoveryServiceId(1L)
            .build();

    DiscoveryService discoveryService =
        DiscoveryService.builder()
            .name("test_discovery_service")
            .id(1L)
            .url("http://discoveryservice.net")
            .build();
    AccessForm accessForm = new AccessForm("test");

    discoveryServiceRepository.save(discoveryService);
    accessFormRepository.save(accessForm);

    String requestBody = TestUtils.jsonFromRequest(Arrays.asList(resourceDTO1, resourceDTO2));
    MvcResult result =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post(RESOURCES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$._embedded.resources[0].name", is(resourceDTO1.getName())))
            .andExpect(
                jsonPath("$._embedded.resources[0].sourceId", is(resourceDTO1.getSourceId())))
            .andExpect(
                jsonPath("$._embedded.resources[0].description", is(resourceDTO1.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.resources[0].contactEmail", is(resourceDTO1.getContactEmail())))
            .andExpect(jsonPath("$._embedded.resources[0].uri", is(resourceDTO1.getUri())))
            .andExpect(jsonPath("$._embedded.resources[1].name", is(resourceDTO2.getName())))
            .andExpect(
                jsonPath("$._embedded.resources[1].sourceId", is(resourceDTO2.getSourceId())))
            .andExpect(
                jsonPath("$._embedded.resources[1].description", is(resourceDTO2.getDescription())))
            .andExpect(
                jsonPath(
                    "$._embedded.resources[1].contactEmail", is(resourceDTO2.getContactEmail())))
            .andExpect(jsonPath("$._embedded.resources[1].uri", is(resourceDTO2.getUri())))
            .andReturn();

    Long id1 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.resources[0].id", Long.class);
    Optional<Resource> resource1 = repository.findById(id1);
    assert resource1.isPresent();
    assertEquals(resourceDTO1.getName(), resource1.get().getName());
    Long id2 =
        JsonPath.parse(result.getResponse().getContentAsString())
            .read("$._embedded.resources[1].id", Long.class);
    Optional<Resource> resource2 = repository.findById(id2);
    assert resource2.isPresent();
    assertEquals(resourceDTO2.getName(), resource2.get().getName());

    ResourceUpdateDTO updatedResourceDTO =
        ResourceUpdateDTO.builder()
            .name("New Resource 3")
            .description("New Resource 3")
            .contactEmail("newres3@test.org")
            .uri("http://newres3.test.org")
            .withdrawn(true)
            .build();
    String updatedRequestBody = TestUtils.jsonFromRequest(updatedResourceDTO);
    MvcResult updatedResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.patch(RESOURCES_ENDPOINT + "/" + resource1.get().getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatedRequestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$.name", is(updatedResourceDTO.getName())))
            .andExpect(jsonPath("$.description", is(updatedResourceDTO.getDescription())))
            .andExpect(jsonPath("$.contactEmail", is(updatedResourceDTO.getContactEmail())))
            .andExpect(jsonPath("$.uri", is(updatedResourceDTO.getUri())))
            .andExpect(jsonPath("$.withdrawn", is(updatedResourceDTO.isWithdrawn())))
            .andReturn();
    Optional<Resource> updatedResource = repository.findById(resource1.get().getId());
    assertEquals(updatedResourceDTO.getName(), updatedResource.get().getName());
    assertEquals(updatedResourceDTO.getDescription(), updatedResource.get().getDescription());
  }

  @Test
  @WithUserDetails("admin")
  void updateResource_validUpdate_ok() throws Exception {
    // Use existing resource from test data
    Long resourceId = 4L;
    Resource existingResource = repository.findById(resourceId).orElseThrow();

    ResourceUpdateDTO updateDTO = ResourceUpdateDTO.builder()
        .name("Updated Resource Name")
        .description("Updated description for testing")
        .contactEmail("updated@test.com")
        .uri("https://updated-resource.test.com")
        .withdrawn(false)
        .build();

    String requestBody = TestUtils.jsonFromRequest(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_ENDPOINT + "/" + resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.name", is(updateDTO.getName())))
        .andExpect(jsonPath("$.description", is(updateDTO.getDescription())))
        .andExpect(jsonPath("$.contactEmail", is(updateDTO.getContactEmail())))
        .andExpect(jsonPath("$.uri", is(updateDTO.getUri())))
        .andExpect(jsonPath("$.withdrawn", is(updateDTO.isWithdrawn())));

    // Verify the resource was actually updated in the database
    Resource updatedResource = repository.findById(resourceId).orElseThrow();
    assertEquals(updateDTO.getName(), updatedResource.getName());
    assertEquals(updateDTO.getDescription(), updatedResource.getDescription());
    assertEquals(updateDTO.getContactEmail(), updatedResource.getContactEmail());
    assertEquals(updateDTO.getUri(), updatedResource.getUri());
    assertEquals(updateDTO.isWithdrawn(), updatedResource.isWithdrawn());
  }

  @Test
  @WithUserDetails("admin")
  void updateResource_partialUpdate_ok() throws Exception {
    // Use existing resource from test data
    Long resourceId = 5L;
    Resource existingResource = repository.findById(resourceId).orElseThrow();
    String originalName = existingResource.getName();
    String originalDescription = existingResource.getDescription();

    // Only update name and withdrawn status
    ResourceUpdateDTO updateDTO = ResourceUpdateDTO.builder()
        .name("Partially Updated Resource")
        .withdrawn(true)
        .build();

    String requestBody = TestUtils.jsonFromRequest(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_ENDPOINT + "/" + resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(content().contentType("application/hal+json"))
        .andExpect(jsonPath("$.name", is(updateDTO.getName())))
        .andExpect(jsonPath("$.withdrawn", is(updateDTO.isWithdrawn())));

    // Verify the resource was updated correctly
    Resource updatedResource = repository.findById(resourceId).orElseThrow();
    assertEquals(updateDTO.getName(), updatedResource.getName());
    assertEquals(updateDTO.isWithdrawn(), updatedResource.isWithdrawn());
    // Verify unchanged fields remain the same
    assertEquals(originalDescription, updatedResource.getDescription());
  }

  @Test
  @WithUserDetails("admin")
  void updateResource_withdrawnStatusToggle_ok() throws Exception {
    // Use existing resource from test data
    Long resourceId = 6L;
    Resource existingResource = repository.findById(resourceId).orElseThrow();
    boolean originalWithdrawnStatus = existingResource.isWithdrawn();

    // Toggle withdrawn status
    ResourceUpdateDTO updateDTO = ResourceUpdateDTO.builder()
        .withdrawn(!originalWithdrawnStatus)
        .build();

    String requestBody = TestUtils.jsonFromRequest(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_ENDPOINT + "/" + resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.withdrawn", is(!originalWithdrawnStatus)));

    // Verify the status was toggled
    Resource updatedResource = repository.findById(resourceId).orElseThrow();
    assertEquals(!originalWithdrawnStatus, updatedResource.isWithdrawn());
  }

  @Test
  @WithUserDetails("admin")
  void updateResource_nonExistentId_notFound() throws Exception {
    Long nonExistentId = 99999L;

    ResourceUpdateDTO updateDTO = ResourceUpdateDTO.builder()
        .name("This should not work")
        .build();

    String requestBody = TestUtils.jsonFromRequest(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_ENDPOINT + "/" + nonExistentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser("researcher")
  void updateResource_asNonAdmin_forbidden() throws Exception {
    // Regular users should not be able to update resources
    Long resourceId = 4L;

    ResourceUpdateDTO updateDTO = ResourceUpdateDTO.builder()
        .name("Unauthorized update attempt")
        .build();

    String requestBody = TestUtils.jsonFromRequest(updateDTO);

    mockMvc
        .perform(
            MockMvcRequestBuilders.patch(RESOURCES_ENDPOINT + "/" + resourceId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isForbidden());
  }
}
