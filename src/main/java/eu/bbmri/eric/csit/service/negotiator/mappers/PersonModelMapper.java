package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserResponseModel;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.Set;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelMapper {

  ModelMapper modelMapper;

  public PersonModelMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<Person, UserResponseModel> typeMap =
        modelMapper.createTypeMap(Person.class, UserResponseModel.class);

    typeMap.addMappings(mapper -> mapper.map(Person::getId, UserResponseModel::setId));
    typeMap.addMappings(
        mapper -> mapper.map(Person::getSubjectId, UserResponseModel::setSubjectId));
    Converter<Set<Resource>, Boolean> resourceToResourceModelConverter =
        q -> convertResourceToResourceModel(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(resourceToResourceModelConverter)
                .map(Person::getResources, UserResponseModel::setRepresentativeOfAnyResource));
  }

  boolean convertResourceToResourceModel(Set<Resource> resources) {
    return Objects.nonNull(resources) && !resources.isEmpty();
  }
}
