package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.dto.OrganizationDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.OrganizationModelAssembler;
import eu.bbmri.eric.csit.service.negotiator.service.OrganizationService;
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
  public PagedModel<EntityModel<OrganizationDTO>> list(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return organizationModelAssembler.toPagedModel(
        (Page<OrganizationDTO>)
            organizationService.findAllOrganizations(PageRequest.of(page, size)));
  }

  @GetMapping("/{id}")
  public EntityModel<OrganizationDTO> findById(@PathVariable("id") Long id) {
    return organizationModelAssembler.toModel(organizationService.findOrganizationById(id));
  }
}
