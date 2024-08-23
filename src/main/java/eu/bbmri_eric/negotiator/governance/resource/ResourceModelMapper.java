package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.user.ResourceResponseModel;
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

    TypeMap<Resource, ResourceResponseModel> resourceToModelTypeMap =
        modelMapper.createTypeMap(Resource.class, ResourceResponseModel.class);
    resourceToModelTypeMap.addMappings(
        mapper -> mapper.map(Resource::getSourceId, ResourceResponseModel::setExternalId));
    resourceToModelTypeMap.addMappings(
        mapper -> mapper.map(Resource::getName, ResourceResponseModel::setName));
    resourceToModelTypeMap.addMappings(
        mapper -> mapper.map(Resource::getId, ResourceResponseModel::setId));
  }
}
