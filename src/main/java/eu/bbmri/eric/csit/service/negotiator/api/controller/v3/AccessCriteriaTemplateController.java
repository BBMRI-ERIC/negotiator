package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation_template.AccessCriteriaDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation_template.AccessCriteriaTemplateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaTemplate;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaTemplateLink;
import eu.bbmri.eric.csit.service.negotiator.service.AccessCriteriaTemplateService;
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
public class AccessCriteriaTemplateController {
  @Autowired private final AccessCriteriaTemplateService accessCriteriaTemplateService;
  @Autowired private final ModelMapper modelMapper;

  public AccessCriteriaTemplateController(
      AccessCriteriaTemplateService accessCriteriaTemplateService, ModelMapper modelMapper) {
    this.accessCriteriaTemplateService = accessCriteriaTemplateService;
    this.modelMapper = modelMapper;

    TypeMap<AccessCriteriaTemplate, AccessCriteriaTemplateDTO> typeMap =
        modelMapper.createTypeMap(AccessCriteriaTemplate.class, AccessCriteriaTemplateDTO.class);

    Converter<Set<AccessCriteriaTemplateLink>, Set<AccessCriteriaDTO>> formFieldConverter =
        ffc -> formFieldConverter(ffc.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(formFieldConverter)
                .map(AccessCriteriaTemplate::getFields, AccessCriteriaTemplateDTO::setFields));
  }

  private Set<AccessCriteriaDTO> formFieldConverter(Set<AccessCriteriaTemplateLink> templateFields) {
    return templateFields.stream()
        .map(
            field ->
                new AccessCriteriaDTO(
                    field.getAccessCriteria().getName(),
                    field.getAccessCriteria().getDescription(),
                    field.getAccessCriteria().getType(),
                    field.getAccessCriteria().getRequired())
        )
        .collect(Collectors.toSet());
  }

  @GetMapping(value = "/access-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  AccessCriteriaTemplateDTO search(@RequestParam String resourceId) {
    AccessCriteriaTemplate accessCriteriaTemplate = accessCriteriaTemplateService.findByResourceEntityId(resourceId);
    return modelMapper.map(accessCriteriaTemplate, AccessCriteriaTemplateDTO.class);
  }
}
