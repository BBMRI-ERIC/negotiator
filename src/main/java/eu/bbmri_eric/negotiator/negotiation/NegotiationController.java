package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.ResourceWithStatusAssembler;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilterDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.UpdateResourcesDTO;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationModelAssembler;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceLifecycleService;
import eu.bbmri_eric.negotiator.user.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Negotiations", description = "Submit and manage access negotiations")
@SecurityRequirement(name = "security_auth")
public class NegotiationController {

  private final NegotiationService negotiationService;

  private final NegotiationLifecycleService negotiationLifecycleService;

  private final ResourceLifecycleService resourceLifecycleService;

  private final PersonService personService;

  private final ResourceService resourceService;

  private final NegotiationModelAssembler assembler;
  private final ResourceWithStatusAssembler resourceWithStatusAssembler;

  public NegotiationController(
      NegotiationService negotiationService,
      NegotiationLifecycleService negotiationLifecycleService,
      ResourceLifecycleService resourceLifecycleService,
      PersonService personService,
      ResourceService resourceService,
      NegotiationModelAssembler assembler,
      ResourceWithStatusAssembler resourceWithStatusAssembler) {
    this.negotiationService = negotiationService;
    this.negotiationLifecycleService = negotiationLifecycleService;
    this.resourceLifecycleService = resourceLifecycleService;
    this.personService = personService;
    this.resourceService = resourceService;
    this.assembler = assembler;
    this.resourceWithStatusAssembler = resourceWithStatusAssembler;
  }

  /** Create a negotiation */
  @PostMapping(
      value = "/negotiations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@Valid @RequestBody NegotiationCreateDTO request) {
    return negotiationService.create(
        request, AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId());
  }

  /**
   * Create a negotiation for a specific project
   *
   * @return NegotiationDTO
   */
  @PutMapping(
      value = "/negotiations/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  NegotiationDTO update(
      @Valid @PathVariable String id, @Valid @RequestBody NegotiationCreateDTO request) {
    if (!isCreator(negotiationService.findById(id, false))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return negotiationService.update(id, request);
  }

  @GetMapping("/negotiations")
  public PagedModel<EntityModel<NegotiationDTO>> list(@Valid @Nullable NegotiationFilterDTO filters) {
    return assembler.toPagedModel(
        (Page<NegotiationDTO>) negotiationService.findAllByFilters(filters), filters, null);
  }

  @GetMapping("/users/{id}/negotiations")
  public PagedModel<EntityModel<NegotiationDTO>> listRelated(
      @Valid @PathVariable Long id, @Valid @Nullable NegotiationFilterDTO filters) {
    checkAuthorization(id);

    return assembler.toPagedModel(
        (Page<NegotiationDTO>) negotiationService.findByFiltersForUser(filters, id), filters, id);
  }

  private static void checkAuthorization(Long id) {
    if (!Objects.equals(AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), id)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Fetch a negotiation
   *
   * @param id of the negotiation
   * @return NegotiationDTO or 403
   */
  @GetMapping("/negotiations/{id}")
  public EntityModel<NegotiationDTO> retrieve(@Valid @PathVariable String id) {
    NegotiationDTO negotiationDTO = negotiationService.findById(id, true);
    boolean isAdmin = AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
    if (isAuthorizedForNegotiation(negotiationDTO)) {
      if (negotiationService.isNegotiationCreator(id) || isAdmin) {
        return assembler.toModelWithRequirementLink(negotiationDTO, isAdmin);
      }
      return assembler.toModel(negotiationDTO);
    } else {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }

  /**
   * Interact with the state of a negotiation by sending an Event
   *
   * @param id of the negotiation
   * @param event from NegotiationEvents
   * @return NegotiationDTO with updated state if valid
   */
  @PutMapping("/negotiations/{id}/lifecycle/{event}")
  public ResponseEntity<?> sendEvent(
      @Valid @PathVariable String id, @Valid @PathVariable("event") NegotiationEvent event) {
    if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()
        && !isCreator(negotiationService.findById(id, false))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    // Process the request
    negotiationLifecycleService.sendEvent(id, event);
    NegotiationDTO result = negotiationService.findById(id, true);
    return ResponseEntity.ok(result);
  }

  /**
   * Interact with the state of a resource in a negotiation by sending an Event
   *
   * @param negotiationId of the Negotiation
   * @param resourceId external it of the resource
   * @param event from NegotiationEvents
   * @return NegotiationDTO with updated state if valid
   */
  @PutMapping("/negotiations/{negotiationId}/resources/{resourceId}/lifecycle/{event}")
  public ResponseEntity<?> sendEventForNegotiationResource(
      @Valid @PathVariable String negotiationId,
      @Valid @PathVariable String resourceId,
      @Valid @PathVariable("event") NegotiationResourceEvent event) {
    if (!personService.isRepresentativeOfAnyResource(
            AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(), List.of(resourceId))
        && !isCreator(negotiationService.findById(negotiationId, false))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    resourceLifecycleService.sendEvent(negotiationId, resourceId, event);
    NegotiationDTO result = negotiationService.findById(negotiationId, true);
    return ResponseEntity.ok(result);
  }

  /**
   * Get possible events for a Negotiation
   *
   * @param id of the negotiation
   * @return a list of possible events to send
   */
  @GetMapping("/negotiations/{id}/lifecycle")
  List<String> getPossibleEvents(@Valid @PathVariable String id) {
    return negotiationLifecycleService.getPossibleEvents(id).stream()
        .map((obj) -> Objects.toString(obj, null))
        .collect(Collectors.toList());
  }

  /**
   * Get possible events for a resource state in a Negotiation
   *
   * @param negotiationId of the negotiation
   * @param resourceId of the resource
   * @return a list of possible events
   */
  @GetMapping("/negotiations/{negotiationId}/resources/{resourceId}/lifecycle")
  List<String> getPossibleEventsForNegotiationResource(
      @Valid @PathVariable String negotiationId, @Valid @PathVariable String resourceId) {
    return resourceLifecycleService.getPossibleEvents(negotiationId, resourceId).stream()
        .map((obj) -> Objects.toString(obj, null))
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/negotiations/{id}/resources")
  @Operation(summary = "List all Resources in negotiation")
  @SecurityRequirement(name = "security_auth")
  public CollectionModel<EntityModel<ResourceWithStatusDTO>> findResourcesForNegotiation(
      @PathVariable String id) {
    if (AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
      return resourceWithStatusAssembler.toCollectionModelWithAdminLinks(
          resourceService.findAllInNegotiation(id), id);
    }
    return resourceWithStatusAssembler.toCollectionModel(resourceService.findAllInNegotiation(id));
  }

  @PatchMapping(value = "/negotiations/{id}/resources")
  @Operation(summary = "Edit Resources linked to a Negotiation")
  @SecurityRequirement(name = "security_auth")
  public CollectionModel<EntityModel<ResourceWithStatusDTO>> updateResources(
      @PathVariable String id, @RequestBody @Valid UpdateResourcesDTO updateResourcesDTO) {
    return resourceWithStatusAssembler.toCollectionModel(
        resourceService.updateResourcesInANegotiation(id, updateResourcesDTO));
  }

  private boolean isAuthorizedForNegotiation(NegotiationDTO negotiationDTO) {
    return isCreator(negotiationDTO)
        || personService.isRepresentativeOfAnyResourceOfNegotiation(
            AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId(),
            negotiationDTO.getId())
        || AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin();
  }

  private String getUserId() {
    String userId = null;
    try {
      userId = AuthenticatedUserContext.getCurrentlyAuthenticatedUserInternalId().toString();
    } catch (ClassCastException e) {
      log.warn("Could not find user in db");
    }
    return userId;
  }

  private boolean isCreator(NegotiationDTO negotiationDTO) {
    return negotiationDTO.getAuthor().getId().equals(getUserId());
  }
}
