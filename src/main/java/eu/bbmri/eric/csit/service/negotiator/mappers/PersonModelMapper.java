package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonDTO;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelMapper {

  @Autowired ModelMapper modelMapper;

  public void addMappings() {
    TypeMap<Person, PersonDTO> typeMap = modelMapper.createTypeMap(Person.class, PersonDTO.class);

    typeMap.addMappings(mapper -> mapper.map(Person::getName, PersonDTO::setName));

    typeMap.addMappings(mapper -> mapper.map(Person::getOrganization, PersonDTO::setOrganization));
  }
}
