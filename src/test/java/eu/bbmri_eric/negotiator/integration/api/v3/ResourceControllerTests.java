package eu.bbmri_eric.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ResourceControllerTests {

  private static final String RESOURCE_ENDPOINT = "/v3/resources/%s";

  private static final String RESOURCES_ENDPOINT = "/v3/resources";

  @Autowired private WebApplicationContext context;
  @Autowired private ResourceRepository repository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  private MockMvc mockMvc;

  @BeforeEach
  public void before() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void getResourceById_validId_ok() throws Exception {
    Resource resource = repository.findById(4L).get();
    mockMvc
        .perform(MockMvcRequestBuilders.get(RESOURCE_ENDPOINT.formatted(4)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(resource.getId().toString())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.externalId", is(resource.getSourceId())));
  }

  @Test
  void getAll_10kResourcesInDb_ok() throws Exception {
    DiscoveryService discoveryService =
        discoveryServiceRepository.save(
            DiscoveryService.builder()
                .url("")
                .name("")
                .build());
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
        .andExpect(jsonPath("$.page.totalElements", is(10006)));
  }
}
