package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import eu.bbmri_eric.negotiator.service.AccessFormElementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/access-forms")
@CrossOrigin
@Tag(name = "Dynamic access forms elements", description = "Manage allowed access form elements")
public class AccessFormElementsController {
  private final AccessFormElementService service;

  public AccessFormElementsController(AccessFormElementService service) {
    this.service = service;
  }

  @GetMapping(value = "/elements", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available elements")
  public CollectionModel<ElementMetaDTO> getAll() {
    return CollectionModel.of(service.getAll());
  }

  @GetMapping(value = "/elements/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an element by id", description = "Returns an element by id")
  public EntityModel<ElementMetaDTO> getElementById(@PathVariable Long id) {
    return EntityModel.of(service.getById(id));
  }
}
