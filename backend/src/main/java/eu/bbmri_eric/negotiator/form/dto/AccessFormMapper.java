package eu.bbmri_eric.negotiator.form.dto;

import eu.bbmri_eric.negotiator.form.AccessForm;
import eu.bbmri_eric.negotiator.form.AccessFormElement;
import eu.bbmri_eric.negotiator.form.AccessFormSection;
import jakarta.annotation.PostConstruct;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessFormMapper {

  ModelMapper modelMapper;

  public AccessFormMapper(ModelMapper modelMapper) {
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

    formTypeMap.addMappings(
        mapping -> mapping.map(AccessForm::getLinkedSections, AccessFormDTO::setSections));
  }

  //  private List<AccessFormSectionDTO> sectionsConverter(Set<AccessFormSectionLink> sections) {
  //    List<AccessFormSectionDTO> newSections = new ArrayList<>();
  //    for (AccessFormSectionLink section : sections) {
  //      newSections.add(
  //          section.getSectionOrder(),
  //          modelMapper.map(section.getAccessFormSection(), AccessFormSectionDTO.class));
  //    }
  //    return newSections;
  //  }
}
