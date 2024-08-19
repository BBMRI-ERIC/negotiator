package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceEventMetadataDto;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceStateMetadataDto;
import eu.bbmri_eric.negotiator.mappers.ResourceEventAssembler;
import eu.bbmri_eric.negotiator.mappers.ResourceStateAssembler;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v3", produces = MediaTypes.HAL_JSON_VALUE)
@CommonsLog
@Tag(name = "Resource Lifecycle", description = "Information about the lifecycle of Resources")
public class ResourceLifecycleController {
  private final ModelMapper modelMapper;
  private final ResourceStateAssembler assembler;
  private final ResourceEventAssembler resourceEventAssembler;
  private final ResourceLifecycleService resourceLifecycleService;

  public ResourceLifecycleController(
      ModelMapper modelMapper,
      ResourceStateAssembler assembler,
      ResourceEventAssembler resourceEventAssembler,
      ResourceLifecycleService resourceLifecycleService) {
    this.modelMapper = modelMapper;
    this.assembler = assembler;
    this.resourceEventAssembler = resourceEventAssembler;
    this.resourceLifecycleService = resourceLifecycleService;
  }

  @GetMapping(value = "/resource-lifecycle/states")
  @Operation(summary = "Retrieve metadata about all possible resource states")
  public CollectionModel<EntityModel<ResourceStateMetadataDto>> getAllStates() {
    return assembler.toCollectionModel(
        Arrays.stream(NegotiationResourceState.class.getEnumConstants())
            .map(resourceState -> modelMapper.map(resourceState, ResourceStateMetadataDto.class))
            .collect(Collectors.toSet()));
  }

  @GetMapping(value = "/resource-lifecycle/states/{state}")
  @Operation(summary = "Retrieve metadata about all a specific resource state")
  public EntityModel<ResourceStateMetadataDto> getState(
      @Valid @PathVariable NegotiationResourceState state) {
    return assembler.toModel(modelMapper.map(state, ResourceStateMetadataDto.class));
  }

  @GetMapping(value = "/resource-lifecycle/events")
  @Operation(summary = "Retrieve metadata about all possible resource events")
  public CollectionModel<EntityModel<ResourceEventMetadataDto>> getAllEvents() {
    return resourceEventAssembler.toCollectionModel(
        Arrays.stream(NegotiationResourceEvent.class.getEnumConstants())
            .map(resourceEvent -> modelMapper.map(resourceEvent, ResourceEventMetadataDto.class))
            .collect(Collectors.toSet()));
  }

  @GetMapping(value = "/resource-lifecycle/events/{event}")
  @Operation(summary = "Retrieve metadata about all a specific resource event")
  public EntityModel<ResourceEventMetadataDto> getEvent(
      @Valid @PathVariable NegotiationResourceEvent event) {
    return resourceEventAssembler.toModel(modelMapper.map(event, ResourceEventMetadataDto.class));
  }

  @GetMapping(value = "/resource-lifecycle")
  @Operation(summary = "Retrieve a state machine diagram with all states and transitions")
  public EntityModel<Map<String, Object>> getStateMachineDiagram() {
    return EntityModel.of(resourceLifecycleService.getStateMachineDiagram());
  }
}
