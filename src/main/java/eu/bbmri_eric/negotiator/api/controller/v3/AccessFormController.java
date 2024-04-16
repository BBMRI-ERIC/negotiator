package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import eu.bbmri_eric.negotiator.mappers.AccessFormModelAssembler;
import eu.bbmri_eric.negotiator.service.AccessCriteriaSetService;
import eu.bbmri_eric.negotiator.service.AccessFormElementService;
import eu.bbmri_eric.negotiator.service.AccessFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
@Tag(name = "Dynamic access forms", description = "Setup and retrieve dynamic access forms")
public class AccessFormController {

  private final AccessCriteriaSetService accessCriteriaSetService;
  private final AccessFormElementService elementService;
  private final AccessFormService accessFormService;
  private final AccessFormModelAssembler accessFormModelAssembler;

  public AccessFormController(
      AccessCriteriaSetService accessCriteriaSetService,
      AccessFormElementService elementService,
      AccessFormService accessFormService,
      AccessFormModelAssembler accessFormModelAssembler) {
    this.accessCriteriaSetService = accessCriteriaSetService;
    this.elementService = elementService;
    this.accessFormService = accessFormService;
    this.accessFormModelAssembler = accessFormModelAssembler;
  }

  @GetMapping(value = "/access-criteria", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Search access criteria",
      description = "Search access criteria by resource id",
      deprecated = true)
  EntityModel<AccessFormDTO> search(@RequestParam String resourceId) {
    return accessFormModelAssembler.toModel(accessCriteriaSetService.findByResourceId(resourceId));
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

  @GetMapping(value = "/access-forms/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an access form by id", description = "Returns an access form by id")
  public EntityModel<AccessFormDTO> findById(@PathVariable Long id) {
    return accessFormModelAssembler.toModel(accessFormService.getAccessForm(id));
  }

  @GetMapping(value = "/access-forms", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get all access forms", description = "List all access forms")
  public PagedModel<EntityModel<AccessFormDTO>> list(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return accessFormModelAssembler.toPagedModel(
        (Page<AccessFormDTO>) accessFormService.getAllAccessForms(PageRequest.of(page, size)));
  }

  @GetMapping(value = "/elements", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available elements")
  public CollectionModel<ElementMetaDTO> getAll() {
    return CollectionModel.of(elementService.getAll());
  }

  @GetMapping(value = "/elements/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an element by id", description = "Returns an element by id")
  public EntityModel<ElementMetaDTO> getElementById(@PathVariable Long id) {
    return EntityModel.of(elementService.getById(id));
  }
}
