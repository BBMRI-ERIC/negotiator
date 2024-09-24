package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceCreateDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceFilterDTO;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Add a list of resources")
  @ResponseStatus(HttpStatus.CREATED)
  public CollectionModel<EntityModel<ResourceResponseModel>> addResources(
      @Valid @RequestBody List<ResourceCreateDTO> resources) {
    return resourceModelAssembler.toCollectionModel(resourceService.addResources(resources));
  }

  @PatchMapping(
      value = "{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Update a resource by id")
  @ResponseStatus(HttpStatus.CREATED)
  public EntityModel<ResourceResponseModel> updateResourceById(
      @PathVariable Long id, @Valid @RequestBody ResourceUpdateDTO resource) {
    return resourceModelAssembler.toModel(resourceService.updateResourceById(id, resource));
  }
}
