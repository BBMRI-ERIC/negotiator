package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.resource.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.ResourceModelMapper;
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
            .dataSource(new DataSource())
            .organization(Organization.builder().externalId("bb:1").name("BB").build())
            .build();
    ResourceDTO resourceDTO = mapper.map(resource, ResourceDTO.class);
    assertEquals(resource.getSourceId(), resourceDTO.getId());
    assertEquals(resource.getName(), resourceDTO.getName());
    assertNotNull(resourceDTO.getOrganization());
    assertEquals(resource.getOrganization().getName(), resourceDTO.getOrganization().getName());
  }
}
