package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.form.FieldDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.form.FormTemplateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.FormFieldTemplateLink;
import eu.bbmri.eric.csit.service.negotiator.database.model.FormTemplate;
import eu.bbmri.eric.csit.service.negotiator.service.FormTemplateService;
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
public class FormTemplateController {
  @Autowired private final FormTemplateService formTemplateService;
  @Autowired private final ModelMapper modelMapper;

  public FormTemplateController(FormTemplateService formTemplateService, ModelMapper modelMapper) {
    this.formTemplateService = formTemplateService;
    this.modelMapper = modelMapper;

    TypeMap<FormTemplate, FormTemplateDTO> typeMap =
        modelMapper.createTypeMap(FormTemplate.class, FormTemplateDTO.class);

    Converter<Set<FormFieldTemplateLink>, Set<FieldDTO>> formFieldConverter =
        ffc -> formFieldConverter(ffc.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(formFieldConverter)
                .map(FormTemplate::getFields, FormTemplateDTO::setFields));
  }

  private Set<FieldDTO> formFieldConverter(Set<FormFieldTemplateLink> templateFields) {
    return templateFields.stream()
        .map(
            field ->
                new FieldDTO(
                    field.getField().getName(),
                    field.getField().getLabel(),
                    field.getField().getType().getName(),
                    field.getField().getRequired())
        )
        .collect(Collectors.toSet());
  }

  @GetMapping(value = "/form-template", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  FormTemplateDTO retrieve(
      @RequestParam Long resourceId) {
    FormTemplate formTemplate = formTemplateService.findByResourceId(resourceId);
    return modelMapper.map(formTemplate, FormTemplateDTO.class);
  }
}
