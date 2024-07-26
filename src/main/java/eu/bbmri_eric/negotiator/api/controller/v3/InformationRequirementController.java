package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import eu.bbmri_eric.negotiator.mappers.InformationRequirementAssembler;
import eu.bbmri_eric.negotiator.service.InformationRequirementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(InformationRequirementController.BASE_URL)
@Tag(
    name = "Information requirements",
    description = "Set requirements for Resource states in Negotiations.")
@SecurityRequirement(name = "security_auth")
public class InformationRequirementController {

  public static final String BASE_URL = "/v3/info-requirements";

  private final InformationRequirementService service;
  private final InformationRequirementAssembler assembler;

  public InformationRequirementController(
      InformationRequirementService service, InformationRequirementAssembler assembler) {
    this.service = service;
    this.assembler = assembler;
  }

  @GetMapping
  @Operation(summary = "List all Information requirements")
  public CollectionModel<EntityModel<InformationRequirementDTO>> findAllRequirements() {
    return assembler.toCollectionModel(service.getAllInformationRequirements());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Find an Information requirement by id")
  public EntityModel<InformationRequirementDTO> findRequirementById(@PathVariable Long id) {
    return assembler.toModel(service.getInformationRequirement(id));
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create an Information requirement")
  public EntityModel<InformationRequirementDTO> createNewRequirement(
      @Valid @RequestBody InformationRequirementCreateDTO createDTO) {
    return assembler.toModel(service.createInformationRequirement(createDTO));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an Information requirement")
  public EntityModel<InformationRequirementDTO> updateRequirement(
      @Valid @RequestBody InformationRequirementCreateDTO createDTO, @PathVariable Long id) {
    return assembler.toModel(service.updateInformationRequirement(createDTO, id));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Delete an Information requirement")
  public void deleteRequirement(@PathVariable Long id) {
    service.deleteInformationRequirement(id);
  }
}
