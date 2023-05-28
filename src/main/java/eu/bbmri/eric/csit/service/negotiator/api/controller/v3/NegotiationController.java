package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationEvent;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationStateService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v3")
@CommonsLog
public class NegotiationController {

  @Autowired
  private NegotiationService negotiationService;

  @Autowired
  private NegotiationStateService negotiationStateService;
  /**
   * Create a negotiation
   */
  @PostMapping(
      value = "/negotiations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@Valid @RequestBody NegotiationCreateDTO request) {
    return negotiationService.create(request, getCreatorId());
  }

  private Long getCreatorId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorUserDetails) auth.getPrincipal()).getPerson().getId();
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
  NegotiationDTO update(@Valid @PathVariable String id,
      @Valid @RequestBody NegotiationCreateDTO request) {
    return negotiationService.update(id, request);
  }

  @GetMapping("/negotiations")
  List<NegotiationDTO> list(
      @RequestParam(required = false) String biobankId,
      @RequestParam(required = false) String collectionId,
      @RequestParam(required = false) String userRole) {
    log.info(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString());
    List<NegotiationDTO> negotiations;
    if (biobankId != null) {
      negotiations = negotiationService.findByBiobankId(biobankId);
    } else if (collectionId != null) {
      negotiations = negotiationService.findByResourceId(collectionId);
    } else if (Objects.equals(userRole, "CREATOR")) {
      negotiations = negotiationService.findByCreatorId(getCreatorId());
    } else if (Objects.equals(userRole, "REPRESENTATIVE")) {
      negotiations = negotiationService.findByResourceIds(getResourceIdsFromUserAuthorities());
      // TODO: Remove this option
    } else {
      negotiations = negotiationService.findAll();
    }
    return negotiations;
  }

  private List<String> getResourceIdsFromUserAuthorities() {
    List<String> resourceIds = new ArrayList<>();
    for(GrantedAuthority grantedAuthority: SecurityContextHolder.getContext().getAuthentication().getAuthorities()){
      // TODO: Fix this for different type of identifiers
      if (grantedAuthority.getAuthority().contains("collection")){
        resourceIds.add(grantedAuthority.getAuthority());
      }
    }
    return Collections.unmodifiableList(resourceIds);
  }

  @GetMapping("/negotiations/{id}")
  NegotiationDTO retrieve(@Valid @PathVariable String id) {
    return negotiationService.findById(id, true);
  }

  @PutMapping("/negotiations/{id}/lifecycle/{event}")
  NegotiationDTO sendEvent(@Valid @PathVariable String id, @Valid @PathVariable String event){
    negotiationStateService.sendEvent(id, NegotiationEvent.valueOf(event));
    return negotiationService.findById(id, true);
  }

  @PutMapping("/negotiations/{negotiationId}/resources/{resourceId}/lifecycle/{event}")
  NegotiationDTO sendEventForNegotiationResource(@Valid @PathVariable String negotiationId,
                                                   @Valid @PathVariable String resourceId,
                                                   @Valid @PathVariable String event){
    negotiationStateService.sendEvent(negotiationId, resourceId, NegotiationEvent.valueOf(event));
    return negotiationService.findById(negotiationId, true);
  }

  @GetMapping("/negotiations/{negotiationId}/resources/{resourceId}/lifecycle")
  List<String> getPossibleEventsForNegotiationResource(@Valid @PathVariable String negotiationId,
                                                       @Valid @PathVariable String resourceId){
    return negotiationStateService
            .getPossibleEvents(negotiationId, resourceId).stream().map((obj) -> Objects.toString(obj, null))
            .collect(Collectors.toList());
  }
  @GetMapping("/negotiations/{id}/lifecycle")
  List<String> getPossibleEvents(@Valid @PathVariable String id){
    return negotiationStateService.getPossibleEvents(id).stream().map((obj) -> Objects.toString(obj, null))
            .collect(Collectors.toList());
  }
}
