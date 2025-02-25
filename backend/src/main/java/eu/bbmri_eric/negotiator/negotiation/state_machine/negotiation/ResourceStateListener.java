package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.common.UserPrincipal;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Listener for Resource State changes. */
@Component
@CommonsLog
public class ResourceStateListener {
  private final NegotiationLifecycleService negotiationLifecycleService;
  private final ResourceService resourceService;
  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;

  public ResourceStateListener(
      NegotiationLifecycleService negotiationLifecycleService,
      ResourceService resourceService,
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository) {
    this.negotiationLifecycleService = negotiationLifecycleService;
    this.resourceService = resourceService;
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
  }

  @EventListener(ResourceStateChangeEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onResourceStateChangeEvent(ResourceStateChangeEvent event) {
    log.info(
        "Resource %s in Negotiation %s had a change of status from %s to %s"
            .formatted(
                event.getResourceId(),
                event.getNegotiationId(),
                event.getFromState(),
                event.getToState()));
    List<ResourceWithStatusDTO> resources =
        resourceService.findAllInNegotiation(event.getNegotiationId());
    if (resources.stream().allMatch(resource -> isDelivered(resource) || isUnavailable(resource))) {
      Person person = personRepository.findById(0L).get();
      Authentication systemAuth =
          new UsernamePasswordAuthenticationToken(
              new UserPrincipal(person), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
      SecurityContextHolder.getContext().setAuthentication(systemAuth);
      concludeNegotiation(event);
      SecurityContextHolder.clearContext();
    }
    ;
  }

  private void concludeNegotiation(ResourceStateChangeEvent event) {
    negotiationLifecycleService.sendEvent(event.getNegotiationId(), NegotiationEvent.CONCLUDE);
  }

  private static boolean isUnavailable(ResourceWithStatusDTO resource) {
    return resource.getCurrentState().equals(NegotiationResourceState.RESOURCE_UNAVAILABLE);
  }

  private static boolean isDelivered(ResourceWithStatusDTO resource) {
    return resource.getCurrentState().equals(NegotiationResourceState.RESOURCE_MADE_AVAILABLE);
  }
}
