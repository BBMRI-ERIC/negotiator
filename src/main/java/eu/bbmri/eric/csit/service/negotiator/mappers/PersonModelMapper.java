package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
import java.util.Set;
import org.modelmapper.Converter;
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
    Converter<Set<Resource>, Boolean> resourceToResourceModelConverter =
        q -> convertResourceToResourceModel(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(resourceToResourceModelConverter)
                .map(Person::getResources, UserModel::setRepresentativeOfAnyResource));
  }

  boolean convertResourceToResourceModel(Set<Resource> resources) {
    return Objects.nonNull(resources) && !resources.isEmpty();
  }
}
