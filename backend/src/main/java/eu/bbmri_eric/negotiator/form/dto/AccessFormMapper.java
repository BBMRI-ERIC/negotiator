package eu.bbmri_eric.negotiator.form.dto;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormElement;
import eu.bbmri_eric.negotiator.form.AccessFormSection;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessFormMapper {

  ModelMapper modelMapper;

  public AccessFormMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    modelMapper.createTypeMap(AccessFormElement.class, AccessFormElementDTO.class);
    modelMapper.createTypeMap(AccessFormSection.class, AccessFormSectionDTO.class);
    modelMapper.createTypeMap(AccessForm.class, AccessFormDTO.class);
  }
}
