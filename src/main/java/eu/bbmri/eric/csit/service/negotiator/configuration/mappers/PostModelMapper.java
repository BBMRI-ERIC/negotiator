package eu.bbmri.eric.csit.service.negotiator.configuration.mappers;


import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostModelMapper {

  @Autowired
  ModelMapper modelMapper;

  void addMappings() {
    TypeMap<Person, PersonDTO> typeMap =
        modelMapper.createTypeMap(Person.class, PersonDTO.class);

    typeMap.addMappings(mapper ->
        mapper.map(Person::getAuthName, PersonDTO::setName));

    typeMap.addMappings(mapper ->
        mapper.map(Person::getOrganization, PersonDTO::setOrganization));

  }




}


