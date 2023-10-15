package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.dto.request.ResourceDTO;
import javax.annotation.PostConstruct;
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

    TypeMap<Resource, ResourceDTO> resourceToDTOTypeMap =
        modelMapper.createTypeMap(Resource.class, ResourceDTO.class);
    resourceToDTOTypeMap.addMappings(
        mapper -> mapper.map(Resource::getSourceId, ResourceDTO::setId));
    resourceToDTOTypeMap.addMappings(mapper -> mapper.map(Resource::getName, ResourceDTO::setName));
  }
}
