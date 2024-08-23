package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationEventMetadataDto;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationStateMetadataDto;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationEventAssembler;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationStateAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Arrays;
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
@Tag(
    name = "Negotiation Lifecycle",
    description = "Information about the lifecycle of Negotiations")
public class NegotiationLifecycleController {
  private final ModelMapper modelMapper;
  private final NegotiationStateAssembler assembler;
  private final NegotiationEventAssembler negotiationEventAssembler;

  public NegotiationLifecycleController(
      ModelMapper modelMapper,
      NegotiationStateAssembler assembler,
      NegotiationEventAssembler negotiationEventAssembler) {
    this.modelMapper = modelMapper;
    this.assembler = assembler;
    this.negotiationEventAssembler = negotiationEventAssembler;
  }

  @GetMapping(value = "/negotiation-lifecycle/states")
  @Operation(summary = "Retrieve metadata about all possible negotiation states")
  public CollectionModel<EntityModel<NegotiationStateMetadataDto>> getAllStates() {
    return assembler.toCollectionModel(
        Arrays.stream(NegotiationState.class.getEnumConstants())
            .map(
                negotiationState ->
                    modelMapper.map(negotiationState, NegotiationStateMetadataDto.class))
            .collect(Collectors.toSet()));
  }

  @GetMapping(value = "/negotiation-lifecycle/states/{state}")
  @Operation(summary = "Retrieve metadata about all a specific negotiation state")
  public EntityModel<NegotiationStateMetadataDto> getState(
      @Valid @PathVariable NegotiationState state) {
    return assembler.toModel(modelMapper.map(state, NegotiationStateMetadataDto.class));
  }

  @GetMapping(value = "/negotiation-lifecycle/events")
  @Operation(summary = "Retrieve metadata about all possible negotiation events")
  public CollectionModel<EntityModel<NegotiationEventMetadataDto>> getAllEvents() {
    return negotiationEventAssembler.toCollectionModel(
        Arrays.stream(NegotiationEvent.class.getEnumConstants())
            .map(
                negotiationState ->
                    modelMapper.map(negotiationState, NegotiationEventMetadataDto.class))
            .collect(Collectors.toSet()));
  }

  @GetMapping(value = "/negotiation-lifecycle/events/{event}")
  @Operation(summary = "Retrieve metadata about all a specific negotiation event")
  public EntityModel<NegotiationEventMetadataDto> getEvent(
      @Valid @PathVariable NegotiationEvent event) {
    return negotiationEventAssembler.toModel(
        modelMapper.map(event, NegotiationEventMetadataDto.class));
  }
}
