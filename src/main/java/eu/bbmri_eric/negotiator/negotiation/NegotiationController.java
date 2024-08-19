package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.ResourceWithStatusAssembler;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilters;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationModelAssembler;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.user.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.user.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Negotiations", description = "Submit and manage access negotiations")
@SecurityRequirement(name = "security_auth")
public class NegotiationController {

  private NegotiationService negotiationService;

  private NegotiationLifecycleService negotiationLifecycleService;

  private ResourceLifecycleService resourceLifecycleService;

  private PersonService personService;

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
    if (!isCreator(negotiationService.findById(id, false))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return negotiationService.update(id, request);
  }

  private void checkNoUnknownParameters(
      Enumeration<String> parameterNames, Set<String> allowedParams) {
    while (parameterNames.hasMoreElements()) {
      String paramName = parameterNames.nextElement();
      if (!allowedParams.contains(paramName)) {
        throw new WrongRequestException("Parameter %s is not allowed".formatted(paramName));
      }
    }
  }

  @GetMapping("/negotiations")
  public PagedModel<EntityModel<NegotiationDTO>> list(
      @Nullable HttpServletRequest request,
      @RequestParam(required = false) List<NegotiationState> status,
      @RequestParam(required = false) LocalDate createdAfter,
      @RequestParam(required = false) LocalDate createdBefore,
      @RequestParam(defaultValue = "creationDate") NegotiationSortField sortBy,
      @RequestParam(defaultValue = "DESC") Sort.Direction sortOrder,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    if (request != null) {
      Set<String> allowedParams =
          new HashSet<>(
              Arrays.asList(
                  "role",
                  "status",
                  "createdAfter",
                  "createdBefore",
                  "sortBy",
                  "sortOrder",
                  "page",
                  "size"));
      Enumeration<String> parameterNames = request.getParameterNames();
      checkNoUnknownParameters(parameterNames, allowedParams);
    }
    NegotiationFilters filters =
        NegotiationFilters.builder()
            .status(status)
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .build();

    return assembler.toPagedModel(
        (Page<NegotiationDTO>)
            negotiationService.findAllByFilters(
                PageRequest.of(page, size, Sort.by(sortOrder, sortBy.name())), filters),
        filters,
        sortBy,
        sortOrder);
  }

  @GetMapping("/users/{id}/negotiations")
  public PagedModel<EntityModel<NegotiationDTO>> listRelated(
      @Nullable HttpServletRequest request,
      @Valid @PathVariable Long id,
      @RequestParam(required = false) NegotiationRole role,
      @RequestParam(required = false) List<NegotiationState> status,
      @RequestParam(required = false) LocalDate createdAfter,
      @RequestParam(required = false) LocalDate createdBefore,
      @RequestParam(defaultValue = "creationDate") NegotiationSortField sortBy,
      @RequestParam(defaultValue = "DESC") Sort.Direction sortOrder,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "50") @Min(1) int size) {
    checkAuthorization(id);

    if (request != null) {
      Set<String> allowedParams =
          new HashSet<>(
              Arrays.asList(
                  "role",
                  "status",
                  "createdAfter",
                  "createdBefore",
                  "sortBy",
                  "sortOrder",
                  "page",
                  "size"));
      Enumeration<String> parameterNames = request.getParameterNames();
      checkNoUnknownParameters(parameterNames, allowedParams);
    }

    NegotiationFilters filters =
        NegotiationFilters.builder()
            .role(role)
            .status(status)
            .createdAfter(createdAfter)
            .createdBefore(createdBefore)
            .build();

    return assembler.toPagedModel(
        (Page<NegotiationDTO>)
            negotiationService.findByFiltersForUser(
                PageRequest.of(page, size, Sort.by(sortOrder, sortBy.name())), filters, id),
        filters,
        sortBy,
        sortOrder,
        id);
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
  public EntityModel<NegotiationDTO> retrieve(@Valid @PathVariable String id) {
    NegotiationDTO negotiationDTO = negotiationService.findById(id, true);
    boolean isAdmin = NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin();
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
  public ResponseEntity<?> sendEventForNegotiationResource(
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
  @Operation(deprecated = true, description = "Replaced by /v3/negotiation-lifecycle/states")
  List<NegotiationState> getPossibleEventsForNegotiationResource() {
    return Arrays.stream(NegotiationState.values()).toList();
  }

  @RequestMapping(value = "/negotiations/{id}/resources", method = RequestMethod.GET)
  @Operation(summary = "List all resources in negotiation")
  @SecurityRequirement(name = "security_auth")
  public CollectionModel<EntityModel<ResourceWithStatusDTO>> findResourcesForNegotiation(
      @PathVariable String id) {
    if (NegotiatorUserDetailsService.isCurrentlyAuthenticatedUserAdmin()) {
      return resourceWithStatusAssembler.toCollectionModelWithAdminLinks(
          resourceService.findAllInNegotiation(id), id);
    }
    return resourceWithStatusAssembler.toCollectionModel(resourceService.findAllInNegotiation(id));
  }

  @RequestMapping(value = "/negotiations/{id}/resources", method = RequestMethod.PATCH)
  @Operation(summary = "Add resources to a negotiation")
  @SecurityRequirement(name = "security_auth")
  public CollectionModel<EntityModel<ResourceWithStatusDTO>> addResourcesForNegotiation(
      @PathVariable String id, @RequestBody @NotEmpty List<Long> resourceIds) {
    return resourceWithStatusAssembler.toCollectionModel(
        resourceService.addResourcesToNegotiation(id, resourceIds));
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
        || personService.isRepresentativeOfAnyResourceOfNegotiation(
            NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId(),
            negotiationDTO.getId())
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
