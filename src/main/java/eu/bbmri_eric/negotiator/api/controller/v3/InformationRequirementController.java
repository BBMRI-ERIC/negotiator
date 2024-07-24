package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.mappers.InformationRequirementAssembler;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Tag(
    name = "Information requirements",
    description = "Set requirements for Resource states in Negotiations.")
@SecurityRequirement(name = "security_auth")
public class InformationRequirementController {

  private final InformationRequirementService service;
  private final InformationRequirementAssembler assembler;

  public InformationRequirementController(
      InformationRequirementService service, InformationRequirementAssembler assembler) {
    this.service = service;
    this.assembler = assembler;
  }

  @GetMapping("/info-requirements")
  @Operation(summary = "List all Information requirements")
  CollectionModel<EntityModel<InformationRequirementDTO>> findAllRequirements() {
    return assembler.toCollectionModel(service.getAllInformationRequirements());
  }

  @GetMapping("/info-requirements/{id}")
  @Operation(summary = "Find an Information requirement by id")
  EntityModel<InformationRequirementDTO> findRequirementById(@PathVariable Long id) {
    return assembler.toModel(service.getInformationRequirement(id));
  }

  @PostMapping("/info-requirements")
  @Operation(summary = "Create an Information requirement by id")
  EntityModel<InformationRequirementDTO> createNewRequirement(
      InformationRequirementCreateDTO createDTO) {
    return assembler.toModel(service.createInformationRequirement(createDTO));
  }

  @PutMapping("/info-requirements/{id}")
  @Operation(summary = "Update an Information requirement")
  EntityModel<InformationRequirementDTO> updateRequirement(
      InformationRequirementCreateDTO createDTO, @PathVariable Long id) {
    return assembler.toModel(service.updateInformationRequirement(createDTO, id));
  }

  @DeleteMapping("/info-requirements/{id}")
  @Operation(summary = "Delete an Information requirement")
  void deleteRequirement(@PathVariable Long id) {
    service.deleteInformationRequirement(id);
  }
}
