package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationStateMetadataDto;
import eu.bbmri_eric.negotiator.mappers.LifecycleModelAssembler;
import eu.bbmri_eric.negotiator.service.LifecycleMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(
    name = "Lifecycle",
    description = "Information about the lifecycle of Negotiations and Resources")
public class LifecycleController {
  private final LifecycleMetadataService metadataService;
  private final LifecycleModelAssembler assembler;

  public LifecycleController(
      LifecycleMetadataService metadataService, LifecycleModelAssembler assembler) {
    this.metadataService = metadataService;
    this.assembler = assembler;
  }

  @GetMapping(value = "/lifecycle/negotiation")
  @Operation(summary = "Get information about the user based on the provided bearer token")
  public CollectionModel<EntityModel<NegotiationStateMetadataDto>> getAllStates() {
    return assembler.toCollectionModel(metadataService.findAllStates());
  }
}
