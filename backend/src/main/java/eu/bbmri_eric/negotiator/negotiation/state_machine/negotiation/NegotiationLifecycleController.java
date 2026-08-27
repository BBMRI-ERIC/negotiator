package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationEventMetadataDTO;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

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
  public CollectionModel<EntityModel<NegotiationEventMetadataDTO>> getAllEvents() {
    return negotiationEventAssembler.toCollectionModel(
        Arrays.stream(NegotiationEvent.class.getEnumConstants())
            .map(
                negotiationState ->
                    modelMapper.map(negotiationState, NegotiationEventMetadataDTO.class))
            .collect(Collectors.toSet()));
  }

  @GetMapping(value = "/negotiation-lifecycle/events/{event}")
  @Operation(summary = "Retrieve metadata about all a specific negotiation event")
  public EntityModel<NegotiationEventMetadataDTO> getEvent(@Valid @PathVariable String event) {
    return negotiationEventAssembler.toModel(
        modelMapper.map(eventNamed(event), NegotiationEventMetadataDTO.class));
  }

  /**
   * Resolves the Event named in the path exactly as the deleted {@code NegotiationEventConverter}
   * did during argument binding - upper-casing the input, and refusing an unknown name with an
   * empty 400. It sits here so the path variable can be a bare {@code String} and the assembler
   * that builds a link to this method needs no Event constant to do it.
   *
   * <p>The Event constant itself stays: this endpoint publishes the metadata of the whole closed
   * Event set, which is the question ticket 04 reopens. It goes when that question is answered.
   */
  private static NegotiationEvent eventNamed(String event) {
    try {
      return NegotiationEvent.valueOf(event.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
  }
}
