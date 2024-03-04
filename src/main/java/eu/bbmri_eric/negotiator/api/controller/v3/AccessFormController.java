package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.mappers.AccessFormModelAssembler;
import eu.bbmri_eric.negotiator.service.AccessCriteriaSetService;
import eu.bbmri_eric.negotiator.service.AccessFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CrossOrigin
@Tag(name = "Access-Form", description = "management and retrieval of access-forms.")
public class AccessFormController {

  @Autowired private AccessCriteriaSetService accessCriteriaSetService;
  @Autowired private AccessFormService accessFormService;
  @Autowired private AccessFormModelAssembler accessFormModelAssembler;

  @GetMapping(value = "/access-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  AccessFormDTO search(@RequestParam String resourceId) {
    return accessCriteriaSetService.findByResourceId(resourceId);
  }

  @GetMapping(value = "/requests/{id}/access-form", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Get an access form for a request",
      description =
          "Returns an access form with sections and"
              + " elements that are relevant for the given resources being requested.")
  public EntityModel<AccessFormDTO> combine(@PathVariable String id) {
    return accessFormModelAssembler.toModel(accessFormService.getAccessFormForRequest(id));
  }
}
