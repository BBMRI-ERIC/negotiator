package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.mappers.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/resources")
public class ResourceController {
  private final ResourceService resourceService;
  private final ResourceModelAssembler resourceModelAssembler;

  public ResourceController(
      ResourceService resourceService, ResourceModelAssembler resourceModelAssembler) {
    this.resourceService = resourceService;
    this.resourceModelAssembler = resourceModelAssembler;
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get resource by id")
  public EntityModel<ResourceResponseModel> getResourceById(@PathVariable("id") Long id) {
    return resourceModelAssembler.toModel(resourceService.findById(id));
  }

  @GetMapping()
  @Operation(summary = "List all resources")
  @SecurityRequirements
  public PagedModel<EntityModel<ResourceResponseModel>> list(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return resourceModelAssembler.toPagedModel(
        (Page<ResourceResponseModel>) resourceService.findAll(PageRequest.of(page, size)));
  }
}
