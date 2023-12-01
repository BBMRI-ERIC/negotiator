package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelMapper {

  @Autowired ModelMapper modelMapper;

  public void addMappings() {
    TypeMap<Person, UserModel> typeMap = modelMapper.createTypeMap(Person.class, UserModel.class);

    typeMap.addMappings(mapper -> mapper.map(Person::getId, UserModel::setId));
    typeMap.addMappings(mapper -> mapper.map(Person::getSubjectId, UserModel::setSubjectId));
  }
}
