package eu.bbmri_eric.negotiator.governance.organization;

import jakarta.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@CommonsLog
public class OrganizationModelMapper {

  @Autowired ModelMapper modelMapper;

  public OrganizationModelMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<Organization, OrganizationDTO> typeMap =
        modelMapper.createTypeMap(Organization.class, OrganizationDTO.class);
  }
}
