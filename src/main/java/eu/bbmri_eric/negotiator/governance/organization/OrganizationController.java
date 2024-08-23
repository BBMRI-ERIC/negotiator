package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.governance.OrganizationDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/v3/organizations")
@Tag(name = "Organizations", description = "Retrieve connected organizations")
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
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return organizationModelAssembler.toPagedModel(
        (Page<OrganizationDTO>)
            organizationService.findAllOrganizations(PageRequest.of(page, size)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get organization by id")
  public EntityModel<OrganizationDTO> findById(@PathVariable("id") Long id) {
    return organizationModelAssembler.toModel(organizationService.findOrganizationById(id));
  }
}
