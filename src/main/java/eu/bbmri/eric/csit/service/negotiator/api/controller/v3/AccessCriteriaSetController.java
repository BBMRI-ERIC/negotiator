package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSetLink;
import eu.bbmri.eric.csit.service.negotiator.service.AccessCriteriaSetService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
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
  @Autowired private final AccessCriteriaSetService accessCriteriaSetService;
  @Autowired private final ModelMapper modelMapper;

  public AccessCriteriaSetController(
      AccessCriteriaSetService accessCriteriaSetService, ModelMapper modelMapper) {
    this.accessCriteriaSetService = accessCriteriaSetService;
    this.modelMapper = modelMapper;

    TypeMap<AccessCriteriaSet, AccessCriteriaSetDTO> typeMap =
        modelMapper.createTypeMap(AccessCriteriaSet.class, AccessCriteriaSetDTO.class);

    Converter<List<AccessCriteriaSetLink>, List<AccessCriteriaDTO>> accessCriteriaConverter =
        ffc -> formFieldConverter(ffc.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(accessCriteriaConverter)
                .map(AccessCriteriaSet::getAccessCriteriaSetLink, AccessCriteriaSetDTO::setAccessCriteria));
  }

  private List<AccessCriteriaDTO> formFieldConverter(List<AccessCriteriaSetLink> accessCriteria) {
    return accessCriteria.stream()
        .map(
            criteria ->
                new AccessCriteriaDTO(
                    criteria.getAccessCriteria().getName(),
                    criteria.getAccessCriteria().getDescription(),
                    criteria.getAccessCriteria().getType(),
                    criteria.getAccessCriteria().getRequired())
        )
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/access-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  AccessCriteriaSetDTO search(@RequestParam String resourceId) {
    AccessCriteriaSet accessCriteriaSet = accessCriteriaSetService.findByResourceEntityId(resourceId);
    return modelMapper.map(accessCriteriaSet, AccessCriteriaSetDTO.class);
  }
}
