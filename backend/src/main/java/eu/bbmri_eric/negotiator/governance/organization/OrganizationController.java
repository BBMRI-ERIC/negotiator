package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.governance.organization.dto.OrganizationFilterDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/organizations")
@Tag(name = "Organizations", description = "Retrieve connected organizations")
@SecurityRequirement(name = "security_auth")
public class OrganizationController {
  private final OrganizationService organizationService;

  private final OrganizationModelAssembler organizationModelAssembler;

  public OrganizationController(
      OrganizationService organizationService,
      OrganizationModelAssembler organizationModelAssembler) {
    this.organizationService = organizationService;
    this.organizationModelAssembler = organizationModelAssembler;
  }

  @GetMapping()
  @Operation(summary = "List all organizations")
  public PagedModel<EntityModel<OrganizationDTO>> list(
      @Valid @Nullable @ParameterObject OrganizationFilterDTO filters) {
    return organizationModelAssembler.toPagedModel(
        (Page<OrganizationDTO>) organizationService.findAllOrganizations(filters), filters);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get organization by id")
  public EntityModel<OrganizationDTO> findById(
      @PathVariable("id") Long id,
      @Schema(description = "resource expansion", example = "resources") String expand) {
    return organizationModelAssembler.toModel(organizationService.findOrganizationById(id, expand));
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Add a list of Organizations")
  @ResponseStatus(HttpStatus.CREATED)
  public CollectionModel<EntityModel<OrganizationDTO>> addOrganizations(
      @Valid @RequestBody List<OrganizationCreateDTO> organizations) {
    return organizationModelAssembler.toCollectionModel(
        organizationService.addOrganizations(organizations));
  }

  @PatchMapping(
      value = "/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Updates an organization by id")
  public EntityModel<OrganizationDTO> updateById(
      @PathVariable("id") Long id, @RequestBody @Valid OrganizationUpdateDTO organization) {
    return organizationModelAssembler.toModel(
        organizationService.updateOrganizationById(id, organization));
  }
}
