package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirement;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementRepository;
import eu.bbmri_eric.negotiator.info_requirement.InformationRequirementService;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmission;
import eu.bbmri_eric.negotiator.info_submission.InformationSubmissionRepository;
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
        .andExpect(jsonPath("$.sourceId", is(resource.getSourceId())));
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
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links").isMap());
  }

  @Test
  @WithMockNegotiatorUser(id = 102L)
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
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
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
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
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
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
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
        .andExpect(jsonPath("$._embedded.resources[0].organization.id").isNumber())
        .andExpect(jsonPath("$._embedded.resources[0].organization.externalId").isString())
        .andExpect(jsonPath("$._embedded.resources[0]._links.submission-1").isNotEmpty())
        .andExpect(jsonPath("$._embedded.resources[0]._links.requirement-1").doesNotExist())
        .andExpect(
            jsonPath("$._embedded.resources[0]._links.MARK_AS_CHECKING_AVAILABILITY").isNotEmpty());
  }

  @Test
  @WithMockUser
  void getResources_filterByName_ok() throws Exception {
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCES_ENDPOINT + "?name=test"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(7)));
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
  void addResourcesBatch() throws Exception {
    ResourceCreateDTO resourceDTO1 =
        ResourceCreateDTO.builder()
            .name("Resource 1")
            .description("Resource 1")
            .sourceId("resource_1")
            .organizationId("test_organization_3")
            .build();
    ResourceCreateDTO resourceDTO2 =
        ResourceCreateDTO.builder()
            .name("Resource 2")
            .description("Resource 2")
            .sourceId("test_resource_2")
            .organizationId("test_organization_4")
            .build();
    Organization org1 =
        Organization.builder()
            .id(Long.valueOf("3"))
            .name("Organization 3")
            .externalId("test_organization_3")
            .build();
    Organization org2 =
        Organization.builder()
            .id(Long.valueOf("4"))
            .name("Organization 4")
            .externalId("test_organization_4")
            .build();
    DiscoveryService discoveryService =
        DiscoveryService.builder()
            .name("test_discovery_service")
            .id(Long.valueOf("1"))
            .url("http://discoveryservice.net")
            .build();
    AccessForm accessForm = new AccessForm("test");
    organizationRepository.save(org1);
    organizationRepository.save(org2);
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
            .andExpect(jsonPath("$[0].name", is(resourceDTO1.getName())))
            .andExpect(jsonPath("$[0].sourceId", is(resourceDTO1.getSourceId())))
            .andExpect(jsonPath("$[0].description", is(resourceDTO1.getDescription())))
            .andExpect(jsonPath("$[0].organization.externalId", is(org1.getExternalId())))
            .andExpect(jsonPath("$[1].name", is(resourceDTO2.getName())))
            .andExpect(jsonPath("$[1].sourceId", is(resourceDTO2.getSourceId())))
            .andExpect(jsonPath("$[1].description", is(resourceDTO2.getDescription())))
            .andExpect(jsonPath("$[1].organization.externalId", is(org2.getExternalId())))
            .andReturn();

    String id1 = JsonPath.parse(result.getResponse().getContentAsString()).read("$[0].sourceId");
    Optional<Resource> resource1 = repository.findBySourceId(id1);
    assert resource1.isPresent();
    assertEquals(resourceDTO1.getName(), resource1.get().getName());
    Long id2 =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$[1].id", Long.class);
    Optional<Resource> resource2 = repository.findById(id2);
    assert resource2.isPresent();
    assertEquals(resourceDTO2.getName(), resource2.get().getName());
  }

  @Test
  @WithUserDetails("admin")
  void updateResource() throws Exception {
    ResourceCreateDTO resourceDTO1 =
        ResourceCreateDTO.builder()
            .name("Resource 3")
            .description("Resource 3")
            .sourceId("resource_3")
            .organizationId("test_organization_1")
            .build();
    ResourceCreateDTO resourceDTO2 =
        ResourceCreateDTO.builder()
            .name("Resource 4")
            .description("Resource 4")
            .sourceId("resource_4")
            .organizationId("test_organization_2")
            .build();
    Organization org1 =
        Organization.builder()
            .id(Long.valueOf("1"))
            .name("Organization 1")
            .externalId("test_organization_1")
            .build();
    Organization org2 =
        Organization.builder()
            .id(Long.valueOf("2"))
            .name("Organization 2")
            .externalId("test_organization_2")
            .build();
    DiscoveryService discoveryService =
        DiscoveryService.builder()
            .name("test_discovery_service")
            .id(Long.valueOf("1"))
            .url("http://discoveryservice.net")
            .build();
    AccessForm accessForm = new AccessForm("test");
    organizationRepository.save(org1);
    organizationRepository.save(org2);
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
            .andExpect(jsonPath("$[0].name", is(resourceDTO1.getName())))
            .andExpect(jsonPath("$[0].sourceId", is(resourceDTO1.getSourceId())))
            .andExpect(jsonPath("$[0].description", is(resourceDTO1.getDescription())))
            .andExpect(jsonPath("$[0].organization.externalId", is(org1.getExternalId())))
            .andExpect(jsonPath("$[1].name", is(resourceDTO2.getName())))
            .andExpect(jsonPath("$[1].sourceId", is(resourceDTO2.getSourceId())))
            .andExpect(jsonPath("$[1].description", is(resourceDTO2.getDescription())))
            .andExpect(jsonPath("$[1].organization.externalId", is(org2.getExternalId())))
            .andReturn();

    Long id1 =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$[0].id", Long.class);
    Optional<Resource> resource1 = repository.findById(id1);
    assert resource1.isPresent();
    assertEquals(resourceDTO1.getName(), resource1.get().getName());
    Long id2 =
        JsonPath.parse(result.getResponse().getContentAsString()).read("$[1].id", Long.class);
    Optional<Resource> resource2 = repository.findById(id2);
    assert resource2.isPresent();
    assertEquals(resourceDTO2.getName(), resource2.get().getName());

    ResourceCreateDTO updatedResourceDTO =
        ResourceCreateDTO.builder()
            .name("New Resource 3")
            .description("New Resource 3")
            .sourceId("resource_3")
            .organizationId("test_organization_3")
            .build();
    String updatedRequestBody = TestUtils.jsonFromRequest(updatedResourceDTO);
    MvcResult updatedResult =
        mockMvc
            .perform(
                MockMvcRequestBuilders.put(RESOURCES_ENDPOINT + "/" + resource1.get().getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(updatedRequestBody))
            .andExpect(status().isCreated())
            .andExpect(content().contentType("application/hal+json"))
            .andExpect(jsonPath("$.name", is(updatedResourceDTO.getName())))
            .andExpect(jsonPath("$.sourceId", is(updatedResourceDTO.getSourceId())))
            .andExpect(jsonPath("$.description", is(updatedResourceDTO.getDescription())))
            .andExpect(jsonPath("$.organization.externalId", is(org1.getExternalId())))
            .andReturn();
    Optional<Resource> updatedResource = repository.findById(resource1.get().getId());
    assertEquals(updatedResourceDTO.getName(), updatedResource.get().getName());
    assertEquals(updatedResourceDTO.getDescription(), updatedResource.get().getDescription());
  }
}
