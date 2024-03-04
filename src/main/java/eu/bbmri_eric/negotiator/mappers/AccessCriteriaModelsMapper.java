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
public class AccessCriteriaModelsMapper {

  @Autowired ModelMapper modelMapper;

  @PostConstruct
  public void addMappings() {
    TypeMap<AccessForm, AccessFormDTO> typeMap =
        modelMapper.createTypeMap(AccessForm.class, AccessFormDTO.class);

    Converter<Set<AccessFormSection>, List<AccessFormSectionDTO>> accessCriteriaConverter =
        ffc -> formFieldConverter(ffc.getSource());

    typeMap.addMappings(
        mapper ->
            mapper
                .using(accessCriteriaConverter)
                .map(AccessForm::getLinkedSections, AccessFormDTO::setSections));
  }

  private List<AccessFormSectionDTO> formFieldConverter(Set<AccessFormSection> sections) {
    return sections.stream()
        .map(
            section -> {
              List<AccessFormElementDTO> accessCriteria =
                  section.getAccessFormElements().stream()
                      .map(
                          criteria ->
                              new AccessFormElementDTO(
                                  criteria.getName(),
                                  criteria.getLabel(),
                                  criteria.getDescription(),
                                  criteria.getType(),
                                  criteria.isRequired()))
                      .toList();
              return new AccessFormSectionDTO(
                  section.getName(), section.getLabel(), section.getDescription(), accessCriteria);
            })
        .collect(Collectors.toList());
  }
}
