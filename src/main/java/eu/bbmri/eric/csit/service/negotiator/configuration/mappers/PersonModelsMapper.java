package eu.bbmri.eric.csit.service.negotiator.configuration.mappers;

import eu.bbmri.eric.csit.service.negotiator.api.dto.perun.PerunUserRequest;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import javax.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelsMapper {

  @Autowired
  ModelMapper modelMapper;

  @PostConstruct
  public void setMappings() {
    TypeMap<PerunUserRequest, Person> propertyMapper =
        modelMapper.createTypeMap(PerunUserRequest.class, Person.class);
    propertyMapper.addMapping(PerunUserRequest::getId, Person::setAuthSubject);
    propertyMapper.addMapping(PerunUserRequest::getMail, Person::setAuthEmail);
    propertyMapper.addMapping(PerunUserRequest::getDisplayName, Person::setAuthName);
    propertyMapper.addMapping(PerunUserRequest::getOrganization, Person::setOrganization);
  }
}
