package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormElementDTO;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormSectionDTO;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class AccessFormModelsMapper {

  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<AccessForm, AccessFormDTO> typeMap =
        modelMapper.createTypeMap(AccessForm.class, AccessFormDTO.class);

    Converter<Set<AccessFormSection>, List<AccessFormSectionDTO>> accessFormElementsConverter =
        ffc -> formFieldConverter(ffc.getSource());

    typeMap.addMappings(
        mapper ->
            mapper
                .using(accessFormElementsConverter)
                .map(AccessForm::getLinkedSections, AccessFormDTO::setSections));
  }

  private List<AccessFormSectionDTO> formFieldConverter(Set<AccessFormSection> sections) {
    return sections.stream()
        .map(
            section -> {
              List<AccessFormElementDTO> accessFormElements =
                  section.getAccessFormElements().stream()
                      .map(
                          accessFormElement ->
                              new AccessFormElementDTO(
                                  accessFormElement.getName(),
                                  accessFormElement.getLabel(),
                                  accessFormElement.getDescription(),
                                  accessFormElement.getType(),
                                  accessFormElement.isRequired()))
                      .toList();
              return new AccessFormSectionDTO(
                  section.getName(),
                  section.getLabel(),
                  section.getDescription(),
                  accessFormElements);
            })
        .collect(Collectors.toList());
  }
}
