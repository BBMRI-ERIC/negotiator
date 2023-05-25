package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.dto.perun.PerunUserDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

@Configuration
public class PersonModelsMapper {

  @Autowired
  ModelMapper modelMapper;

  @PostConstruct
  public void setMappings() {
    TypeMap<PerunUserDTO, Person> propertyMapper =
        modelMapper.createTypeMap(PerunUserDTO.class, Person.class);
    propertyMapper.addMapping(PerunUserDTO::getId, Person::setAuthSubject);
    propertyMapper.addMapping(PerunUserDTO::getMail, Person::setAuthEmail);
    propertyMapper.addMapping(PerunUserDTO::getDisplayName, Person::setAuthName);
    propertyMapper.addMapping(PerunUserDTO::getOrganization, Person::setOrganization);
  }
}
