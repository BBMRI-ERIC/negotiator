package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.Assert.assertEquals;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceCreateModelMapper;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class ResourceCreateModelMapperTest {

  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks ResourceCreateModelMapper resourceCreateModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.resourceCreateModelMapper.addMappings();
  }

  @Test
  void createDtoToResource_map_ok() {
    ResourceCreateDTO resCreateDto =
        ResourceCreateDTO.builder()
            .name("resource")
            .description("resource desc")
            .sourceId("resource1")
            .contactEmail("resource@res.test")
            .uri("http://resource.org")
            .build();
    Resource resource = mapper.map(resCreateDto, Resource.class);
    assertEquals(resource.getName(), resCreateDto.getName());
    assertEquals(resource.getDescription(), resCreateDto.getDescription());
    assertEquals(resource.getSourceId(), resCreateDto.getSourceId());
    assertEquals(resource.getContactEmail(), resCreateDto.getContactEmail());
    assertEquals(resource.getUri(), resCreateDto.getUri());
  }
}
