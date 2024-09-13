package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceFilterDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/resources")
@Tag(name = "Resources", description = "Retrieve offered resources")
@CommonsLog
@SecurityRequirement(name = "security_auth")
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

  @GetMapping
  @Operation(summary = "List all resources")
  public PagedModel<EntityModel<ResourceResponseModel>> list(@Nullable ResourceFilterDTO filters) {
    assert filters != null;
    return resourceModelAssembler.toPagedModel(
        (Page<ResourceResponseModel>) resourceService.findAll(filters), filters);
  }

  @PostMapping
  @Operation(summary = "Add a list of resources")
  public Iterable<ResourceDTO> addResources(@Valid @RequestBody List<ResourceCreateDTO> resources) {
    return resourceService.addResources(resources);
  }

  @PutMapping(value = "{id}")
  @Operation(summary = "Update a resource by id")
  public ResourceDTO updateResourceById(
      @PathVariable Long id, @Valid @RequestBody ResourceCreateDTO resource) {
    return resourceService.updateResourceById(id, resource);
  }
}
