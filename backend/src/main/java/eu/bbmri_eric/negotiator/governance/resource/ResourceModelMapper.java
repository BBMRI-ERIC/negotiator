package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithRepsDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceModelMapper {

  ModelMapper modelMapper;

  public ResourceModelMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

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
    TypeMap<Resource, ResourceWithRepsDTO> withResourcesDTOTypeMap =
        modelMapper.createTypeMap(Resource.class, ResourceWithRepsDTO.class);
    withResourcesDTOTypeMap.addMappings(
        mapper ->
            mapper.map(Resource::getRepresentatives, ResourceWithRepsDTO::setRepresentatives));
  }
}
