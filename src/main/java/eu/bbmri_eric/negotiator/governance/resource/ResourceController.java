package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceFilterDTO;
import eu.bbmri_eric.negotiator.user.ResourceResponseModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    log.info("Listing resources with filters: " + filters);
    return null;
  }
}
