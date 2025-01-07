package eu.bbmri_eric.negotiator.governance.organization;

import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrganizationCreateModelMapper {

  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<OrganizationCreateDTO, Organization> typeMap =
        modelMapper.getTypeMap(OrganizationCreateDTO.class, Organization.class);
    if (typeMap == null) {
      typeMap = modelMapper.createTypeMap(OrganizationCreateDTO.class, Organization.class);
    }
    typeMap.addMappings(
        mapper -> mapper.map(OrganizationCreateDTO::getExternalId, Organization::setExternalId));
    typeMap.addMappings(
        mapper -> mapper.map(OrganizationCreateDTO::getName, Organization::setName));
    typeMap.addMappings(mapper -> mapper.map(OrganizationCreateDTO::getUri, Organization::setUri));
    typeMap.addMappings(
        mapper -> mapper.map(OrganizationCreateDTO::getWithdrawn, Organization::setWithdrawn));
    typeMap.addMappings(
        mapper ->
            mapper.map(OrganizationCreateDTO::getContactEmail, Organization::setContactEmail));
    typeMap.addMappings(mapper -> mapper.skip(Organization::setId));
  }
}
