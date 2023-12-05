package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceModel;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import jakarta.annotation.PostConstruct;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelMapper {

  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<Person, UserModel> typeMap = modelMapper.createTypeMap(Person.class, UserModel.class);

    typeMap.addMappings(mapper -> mapper.map(Person::getId, UserModel::setId));
    typeMap.addMappings(mapper -> mapper.map(Person::getSubjectId, UserModel::setSubjectId));
    // Converter<Set<Resource>, Set<ResourceModel>> resourceToResourceModelConverter = q ->
    // convertResourceToResourceModel(q.getSource());
    // typeMap.addMappings(mapper ->
    // mapper.using(resourceToResourceModelConverter).map(Person::getResources,
    // UserModel::setRepresentedResources));
  }

  Set<ResourceModel> convertResourceToResourceModel(Set<Resource> resources) {
    if (resources == null) return Set.of();
    return resources.stream()
        .map(resource -> modelMapper.map(resource, ResourceModel.class))
        .collect(java.util.stream.Collectors.toSet());
  }
}
