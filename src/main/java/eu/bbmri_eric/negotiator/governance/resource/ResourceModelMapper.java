package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceModelMapper {

  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<ResourceDTO, Resource> typeMap =
        modelMapper.createTypeMap(ResourceDTO.class, Resource.class);

    typeMap.addMappings(mapper -> mapper.map(ResourceDTO::getId, Resource::setSourceId));

    typeMap.addMappings(mapper -> mapper.map(ResourceDTO::getName, Resource::setName));

    typeMap.addMappings(mapper -> mapper.skip(Resource::setId));

    TypeMap<Resource, ResourceDTO> resourceToDTOTypeMap =
        modelMapper.createTypeMap(Resource.class, ResourceDTO.class);

    resourceToDTOTypeMap.addMappings(
        mapper -> mapper.map(Resource::getSourceId, ResourceDTO::setId));

    resourceToDTOTypeMap.addMappings(mapper -> mapper.map(Resource::getName, ResourceDTO::setName));

    TypeMap<MolgenisCollection, Resource> molgenisCollectionResourceTypeMap =
        modelMapper.createTypeMap(MolgenisCollection.class, Resource.class);
    molgenisCollectionResourceTypeMap.addMappings(
        mapper -> mapper.map(MolgenisCollection::getId, Resource::setSourceId));
    molgenisCollectionResourceTypeMap.addMappings(mapper -> mapper.skip(Resource::setId));
  }

  public static ResourceDTO toDto(Resource resource) {
    ResourceDTO resDTO =
        ResourceDTO.builder()
            .id(resource.getId().toString())
            .name(resource.getName())
            .sourceId(resource.getSourceId())
            .build();
    return resDTO;
  }

  public static List<ResourceDTO> toDtoList(List<Resource> resources) {
    return resources.stream().map(ResourceModelMapper::toDto).collect(Collectors.toList());
  }
}
