package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ResourceCreateModelMapper {
  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    modelMapper.getConfiguration().setAmbiguityIgnored(true);
    TypeMap<ResourceCreateDTO, Resource> typeMap =
        modelMapper.createTypeMap(ResourceCreateDTO.class, Resource.class);
    typeMap.addMappings(
        mapper -> mapper.map(ResourceCreateDTO::getSourceId, Resource::setSourceId));
    typeMap.addMappings(mapper -> mapper.map(ResourceCreateDTO::getName, Resource::setName));
    typeMap.addMappings(
        mapper -> mapper.map(ResourceCreateDTO::getDescription, Resource::setDescription));
    typeMap.addMappings(mapper -> mapper.map(ResourceCreateDTO::getUri, Resource::setUri));
    typeMap.addMappings(
        mapper -> mapper.map(ResourceCreateDTO::getContactEmail, Resource::setContactEmail));

    typeMap.addMappings(mapper -> mapper.skip(Resource::setId));
  }
}
