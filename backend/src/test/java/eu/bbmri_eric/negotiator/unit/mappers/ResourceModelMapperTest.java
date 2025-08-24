package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelMapper;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithRepsDTO;
import eu.bbmri_eric.negotiator.user.Person;
import java.util.Set;
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

  @Test
  void resourceToDTOWithReps_map_ok() {
    // Arrange: Create a Resource instance
    Resource resource =
        Resource.builder()
            .id(1L)
            .name("Test Resource")
            .description("This is a test resource.")
            .sourceId("resource:123")
            .contactEmail("test@resource.org")
            .uri("https://resource.org")
            .build();

    // Add representatives
    Person rep1 =
        Person.builder()
            .subjectId("1")
            .id(1L)
            .name("Sarah Rep")
            .email("sarah.rep@example.com")
            .build();
    Person rep2 =
        Person.builder()
            .subjectId("2")
            .id(2L)
            .name("Adam Rep")
            .email("adam.rep@example.com")
            .build();
    Person rep3 =
        Person.builder()
            .subjectId("3")
            .id(3L)
            .name("John Rep")
            .email("john.rep@example.com")
            .build();
    resource.setRepresentatives(Set.of(rep1, rep3, rep2));

    // Act: Map Resource to ResourceWithRepsDTO
    ResourceWithRepsDTO dto = mapper.map(resource, ResourceWithRepsDTO.class);

    // Assert: Validate the mapped DTO fields
    assertNotNull(dto);
    assertEquals(resource.getName(), dto.getName());
    assertEquals(resource.getDescription(), dto.getDescription());
    assertEquals(resource.getSourceId(), dto.getSourceId());
    assertEquals(resource.getContactEmail(), dto.getContactEmail());
    assertEquals(resource.getUri(), dto.getUri());
    assertEquals(3, dto.getRepresentatives().size());
    assertTrue(dto.getRepresentatives().stream().anyMatch(r -> r.getId().equals(1L)));
  }
}
