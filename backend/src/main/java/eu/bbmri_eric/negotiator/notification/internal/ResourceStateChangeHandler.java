package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.EnumBackedLifecycleCatalog;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResourceStateChangeHandler implements NotificationStrategy<ResourceStateChangeEvent> {

  public static final String TITLE = "Request Status update";
  public static final String BODY =
      "Resource %s had a change of status in your request %s, from %s to %s";
  private final NegotiationRepository negotiationRepository;
  private final NotificationService notificationService;
  private final EnumBackedLifecycleCatalog lifecycleCatalog;

  public ResourceStateChangeHandler(
      NegotiationRepository negotiationRepository,
      NotificationService notificationService,
      EnumBackedLifecycleCatalog lifecycleCatalog) {
    this.negotiationRepository = negotiationRepository;
    this.notificationService = notificationService;
    this.lifecycleCatalog = lifecycleCatalog;
  }

  @Override
  public Class<ResourceStateChangeEvent> getSupportedEventType() {
    return ResourceStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(ResourceStateChangeEvent event) {
    Negotiation negotiation =
        negotiationRepository
            .findById(event.getNegotiationId())
            .orElseThrow(() -> new EntityNotFoundException(event.getNegotiationId()));
    notificationService.createNotifications(
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()),
            TITLE,
            BODY.formatted(
                event.getResourceId(),
                negotiation.getTitle(),
                resourceStateLabel(event.getFromState().name()),
                resourceStateLabel(event.getToState().name())),
            event.getNegotiationId()));
  }

  /**
   * Reads a Resource State's human label off the catalog rather than off the enum this handler is
   * about to lose. Replaced by the label on the named {@code state} row at the Lifecycle cutover.
   */
  private String resourceStateLabel(String stateName) {
    return lifecycleCatalog
        .metadata(
            EnumBackedLifecycleCatalog.Scope.RESOURCE,
            EnumBackedLifecycleCatalog.Element.STATE,
            stateName)
        .label();
  }
}
