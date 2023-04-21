package eu.bbmri.eric.csit.service.negotiator.configuration.mappers;

import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSectionDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSection;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessCriteriaModelsMapper {

  @Autowired
  ModelMapper modelMapper;

  @PostConstruct
  void addMappings() {
    TypeMap<AccessCriteriaSet, AccessCriteriaSetDTO> typeMap =
        modelMapper.createTypeMap(AccessCriteriaSet.class, AccessCriteriaSetDTO.class);

    Converter<Set<AccessCriteriaSection>, List<AccessCriteriaSectionDTO>> accessCriteriaConverter =
        ffc -> formFieldConverter(ffc.getSource());

    typeMap.addMappings(
        mapper ->
            mapper
                .using(accessCriteriaConverter)
                .map(AccessCriteriaSet::getSections, AccessCriteriaSetDTO::setSections));
  }

  private List<AccessCriteriaSectionDTO> formFieldConverter(Set<AccessCriteriaSection> sections) {
    return sections.stream()
        .map(
            section -> {
              List<AccessCriteriaDTO> accessCriteria = section.getAccessCriteriaSectionLink()
                  .stream().map(
                      criteria -> new AccessCriteriaDTO(
                          criteria.getAccessCriteria().getName(),
                          criteria.getAccessCriteria().getLabel(),
                          criteria.getAccessCriteria().getDescription(),
                          criteria.getAccessCriteria().getType(),
                          criteria.getRequired())
                  ).toList();
              return new AccessCriteriaSectionDTO(
                  section.getName(),
                  section.getLabel(),
                  section.getDescription(),
                  accessCriteria);
            }
        )
        .collect(Collectors.toList());
  }
}
