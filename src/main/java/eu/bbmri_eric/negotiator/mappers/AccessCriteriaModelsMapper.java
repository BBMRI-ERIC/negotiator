package eu.bbmri_eric.negotiator.mappers;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaDTO;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSectionDTO;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
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
    TypeMap<AccessForm, AccessCriteriaSetDTO> typeMap =
        modelMapper.createTypeMap(AccessForm.class, AccessCriteriaSetDTO.class);

    Converter<Set<AccessFormSection>, List<AccessCriteriaSectionDTO>> accessCriteriaConverter =
        ffc -> formFieldConverter(ffc.getSource());

    typeMap.addMappings(
        mapper ->
            mapper
                .using(accessCriteriaConverter)
                .map(AccessForm::getSections, AccessCriteriaSetDTO::setSections));
  }

  private List<AccessCriteriaSectionDTO> formFieldConverter(Set<AccessFormSection> sections) {
    return sections.stream()
        .map(
            section -> {
              List<AccessCriteriaDTO> accessCriteria =
                  section.getAccessFormElements().stream()
                      .map(
                          criteria ->
                              new AccessCriteriaDTO(
                                  criteria.getName(),
                                  criteria.getLabel(),
                                  criteria.getDescription(),
                                  criteria.getType(),
                                  false))
                      .toList();
              return new AccessCriteriaSectionDTO(
                  section.getName(), section.getLabel(), section.getDescription(), accessCriteria);
            })
        .collect(Collectors.toList());
  }
}
