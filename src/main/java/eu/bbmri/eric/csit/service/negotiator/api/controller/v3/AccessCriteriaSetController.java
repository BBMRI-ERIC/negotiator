package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSectionDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSection;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSectionLink;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri.eric.csit.service.negotiator.database.model.Project;
import eu.bbmri.eric.csit.service.negotiator.service.AccessCriteriaSetService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
public class AccessCriteriaSetController {

  private final AccessCriteriaSetService accessCriteriaSetService;

  private final ModelMapper modelMapper;

  public AccessCriteriaSetController(
      AccessCriteriaSetService accessCriteriaSetService, ModelMapper modelMapper) {
    this.accessCriteriaSetService = accessCriteriaSetService;
    this.modelMapper = modelMapper;

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
              List<AccessCriteriaDTO> accessCriteria = section.getAccessCriteriaSectionLink().stream().map(
                  criteria -> new AccessCriteriaDTO(
                      criteria.getAccessCriteria().getName(),
                      criteria.getAccessCriteria().getDescription(),
                      criteria.getAccessCriteria().getType(),
                      criteria.getRequired())
              ).toList();
              return new AccessCriteriaSectionDTO(
                  section.getTitle(),
                  section.getDescription(),
                  accessCriteria);
            }
        )
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/access-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  AccessCriteriaSetDTO search(@RequestParam String resourceId) {
    AccessCriteriaSet accessCriteriaSet = accessCriteriaSetService.findByResourceEntityId(
        resourceId);
    return modelMapper.map(accessCriteriaSet, AccessCriteriaSetDTO.class);
  }
}
