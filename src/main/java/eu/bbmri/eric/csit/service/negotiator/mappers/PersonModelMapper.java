package eu.bbmri.eric.csit.service.negotiator.mappers;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersonModelMapper {

  @Autowired ModelMapper modelMapper;

  public void addMappings() {}
}
