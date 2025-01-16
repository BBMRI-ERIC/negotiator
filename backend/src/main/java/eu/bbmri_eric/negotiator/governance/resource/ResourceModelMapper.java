package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import jakarta.annotation.PostConstruct;
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

    typeMap.addMappings(
        mapper -> mapper.map(ResourceDTO::getDescription, Resource::setDescription));

    typeMap.addMappings(
        mapper -> mapper.map(ResourceDTO::getContactEmail, Resource::setDescription));

    typeMap.addMappings(mapper -> mapper.map(ResourceDTO::getUri, Resource::setUri));

    typeMap.addMappings(mapper -> mapper.skip(Resource::setId));

    TypeMap<Resource, ResourceDTO> resourceToDTOTypeMap =
        modelMapper.createTypeMap(Resource.class, ResourceDTO.class);

    resourceToDTOTypeMap.addMappings(
        mapper -> mapper.map(Resource::getSourceId, ResourceDTO::setId));

    resourceToDTOTypeMap.addMappings(mapper -> mapper.map(Resource::getName, ResourceDTO::setName));

    resourceToDTOTypeMap.addMappings(
        mapper -> mapper.map(Resource::getDescription, ResourceDTO::setDescription));

    resourceToDTOTypeMap.addMappings(
        mapper -> mapper.map(Resource::getContactEmail, ResourceDTO::setContactEmail));

    resourceToDTOTypeMap.addMappings(mapper -> mapper.map(Resource::getUri, ResourceDTO::setUri));
  }
}
