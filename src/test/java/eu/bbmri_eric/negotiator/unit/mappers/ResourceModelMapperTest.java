package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelMapper;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class ResourceModelMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks ResourceModelMapper resourceModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.resourceModelMapper.addMappings();
  }

  @Test
  void resourceToDTO_map_Ok() {
    Resource resource =
        Resource.builder()
            .sourceId("test:collection")
            .name("My collection")
            .discoveryService(new DiscoveryService())
            .organization(Organization.builder().externalId("bb:1").name("BB").build())
            .build();
    ResourceDTO resourceDTO = mapper.map(resource, ResourceDTO.class);
    assertEquals(resource.getSourceId(), resourceDTO.getId());
    assertEquals(resource.getName(), resourceDTO.getName());
    assertNotNull(resourceDTO.getOrganization());
    assertEquals(resource.getOrganization().getName(), resourceDTO.getOrganization().getName());
  }

  @Test
  void resourceToResponseModel_map_Ok() {
    Resource resource =
        Resource.builder()
            .sourceId("test:collection")
            .name("My collection")
            .discoveryService(new DiscoveryService())
            .organization(Organization.builder().externalId("bb:1").name("BB").build())
            .contactEmail("test@test.org")
            .uri("http://test.org")
            .build();
    ResourceResponseModel resourceResponseModel = mapper.map(resource, ResourceResponseModel.class);
    assertEquals(resource.getSourceId(), resourceResponseModel.getSourceId());
    assertEquals(resource.getName(), resourceResponseModel.getName());
    assertEquals(resource.getContactEmail(), resourceResponseModel.getContactEmail());
    assertEquals(resource.getUri(), resourceResponseModel.getUri());
  }
}
