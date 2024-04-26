package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormElementDTO;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormSectionDTO;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessFormModelsMapper {

  ModelMapper modelMapper;

  public AccessFormModelsMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<AccessFormElement, AccessFormElementDTO> elementTypeMap =
        modelMapper.createTypeMap(AccessFormElement.class, AccessFormElementDTO.class);
    TypeMap<AccessFormSection, AccessFormSectionDTO> sectionTypeMap =
        modelMapper.createTypeMap(AccessFormSection.class, AccessFormSectionDTO.class);
    TypeMap<AccessForm, AccessFormDTO> formTypeMap =
        modelMapper.createTypeMap(AccessForm.class, AccessFormDTO.class);
  }
}
