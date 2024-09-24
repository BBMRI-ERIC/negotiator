package eu.bbmri_eric.negotiator.notification.researcher;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ResearcherNotificationListener {
  private final ResearcherNotificationService notificationService;

  public ResearcherNotificationListener(ResearcherNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @EventListener(value = NegotiationStateChangeEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void handleStatusChangeEvent(NegotiationStateChangeEvent event) {
    if (event.getEvent().equals(NegotiationEvent.APPROVE)
        || event.getEvent().equals(NegotiationEvent.DECLINE)) {
      notificationService.statusChangeNotification(event.getNegotiationId(), event.getEvent());
    }
  }
}
