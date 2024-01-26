package eu.bbmri.eric.csit.service.negotiator.integration.api.v3;

import static org.hamcrest.core.Is.is;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.OrganizationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
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
  @Autowired private DataSourceRepository dataSourceRepository;
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
    DataSource dataSource =
        dataSourceRepository.save(
            DataSource.builder()
                .sourcePrefix("")
                .apiPassword("")
                .apiType(DataSource.ApiType.MOLGENIS)
                .apiUrl("")
                .apiUsername("")
                .url("")
                .resourceBiobank("")
                .resourceCollection("")
                .resourceNetwork("")
                .name("")
                .syncActive(true)
                .build());
    for (int i = 1000; i < 11000; i++) {
      Organization organization =
          organizationRepository.save(
              Organization.builder().name("test").externalId("biobank:%s".formatted(i)).build());
      repository.save(
          Resource.builder()
              .organization(organization)
              .dataSource(dataSource)
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
