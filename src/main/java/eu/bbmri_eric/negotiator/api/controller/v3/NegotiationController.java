package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.mappers.NegotiationModelAssembler;
import eu.bbmri_eric.negotiator.service.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import eu.bbmri_eric.negotiator.service.PersonService;
import eu.bbmri_eric.negotiator.service.RepresentativeNegotiationService;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Negotiations", description = "management of negotiations and their content")
public class NegotiationController {

  @Autowired private NegotiationService negotiationService;

  @Autowired private NegotiationLifecycleService negotiationLifecycleService;

  @Autowired private ResourceLifecycleService resourceLifecycleService;

  @Autowired private RepresentativeNegotiationService representativeNegotiationService;

  @Autowired private PersonService personService;

  private final NegotiationModelAssembler assembler = new NegotiationModelAssembler();

  /** Create a negotiation */
  @PostMapping(
      value = "/negotiations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@Valid @RequestBody NegotiationCreateDTO request) {
    return negotiationService.create(
        request, NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId());
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
    return negotiationService.update(id, request);
  }

  @GetMapping("/negotiations")
  public PagedModel<EntityModel<NegotiationDTO>> list(
      @RequestParam(required = false) NegotiationState status,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    if (Objects.nonNull(status)) {
      return assembler.toPagedModel(
          (Page<NegotiationDTO>)
              negotiationService.findAllByCurrentStatus(PageRequest.of(page, size), status),
          status);
    }
    return assembler.toPagedModel(
        (Page<NegotiationDTO>) negotiationService.findAll(PageRequest.of(page, size)), status);
  }

  @GetMapping("/users/{id}/negotiations")
  public PagedModel<EntityModel<NegotiationDTO>> listRelated(
      @Valid @PathVariable Long id,
      @RequestParam(required = false) @Valid NegotiationRole role,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    checkAuthorization(id);
    if (Objects.isNull(role)) {
      return assembler.toPagedModel(
          (Page<NegotiationDTO>)
              negotiationService.findAllRelatedTo(
                  PageRequest.of(page, size, Sort.by("creationDate").descending()), id),
          role,
          id);
    } else if (role == NegotiationRole.AUTHOR) {
      return assembler.toPagedModel(
          (Page<NegotiationDTO>)
              negotiationService.findAllCreatedBy(
                  PageRequest.of(page, size, Sort.by("creationDate").descending()), id),
          role,
          id);
    } else if (role == NegotiationRole.REPRESENTATIVE) {
      return assembler.toPagedModel(
          (Page<NegotiationDTO>)
              representativeNegotiationService.findNegotiationsConcerningRepresentative(
                  PageRequest.of(page, size, Sort.by("creationDate").descending()), id),
          role,
          id);
    }
    return PagedModel.empty();
  }

  private static void checkAuthorization(Long id) {
    if (!Objects.equals(
        NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(), id)) {
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
  public NegotiationDTO retrieve(@Valid @PathVariable String id) {
    NegotiationDTO negotiationDTO = negotiationService.findById(id, true);
    if (isAuthorizedForNegotiation(negotiationDTO)) {
      return negotiationDTO;
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
  ResponseEntity<?> sendEvent(
      @Valid @PathVariable String id, @Valid @PathVariable("event") NegotiationEvent event) {
    if (!NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin()
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
  ResponseEntity<?> sendEventForNegotiationResource(
      @Valid @PathVariable String negotiationId,
      @Valid @PathVariable String resourceId,
      @Valid @PathVariable("event") NegotiationResourceEvent event) {
    if (!personService.isRepresentativeOfAnyResource(
            NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
            List.of(resourceId))
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

  @GetMapping("/negotiations/lifecycle")
  List<NegotiationState> getPossibleEventsForNegotiationResource() {
    return Arrays.stream(NegotiationState.values()).toList();
  }

  private List<String> getResourceIdsFromUserAuthorities() {
    List<String> resourceIds = new ArrayList<>();
    for (GrantedAuthority grantedAuthority :
        SecurityContextHolder.getContext().getAuthentication().getAuthorities()) {
      // Edit for different groups/resource types
      if (grantedAuthority.getAuthority().contains("collection")) {
        resourceIds.add(grantedAuthority.getAuthority().replace("ROLE_REPRESENTATIVE_", ""));
      }
    }
    return Collections.unmodifiableList(resourceIds);
  }

  private boolean isAuthorizedForNegotiation(NegotiationDTO negotiationDTO) {
    return isCreator(negotiationDTO)
        || personService.isRepresentativeOfAnyResource(
            NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
            negotiationDTO.getResources().stream()
                .map(ResourceWithStatusDTO::getId)
                .collect(Collectors.toList()))
        || NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
  }

  private String getUserId() {
    String userId = null;
    try {
      userId = NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId().toString();
    } catch (ClassCastException e) {
      log.warn("Could not find user in db");
    }
    return userId;
  }

  private boolean isCreator(NegotiationDTO negotiationDTO) {
    return negotiationDTO.getAuthor().getId().equals(getUserId());
  }
}