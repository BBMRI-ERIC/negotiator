package eu.bbmri_eric.negotiator.notification.researcher;

import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@CommonsLog
public class ResearcherNotificationListener {
  private ResearcherNotificationService notificationService;

  protected ResearcherNotificationListener() {}

  @Autowired
  public ResearcherNotificationListener(ResearcherNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @EventListener(value = NegotiationStateChangeEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  @Async
  public void handleStatusChangeEvent(NegotiationStateChangeEvent event) {
    if (event.getEvent().equals(NegotiationEvent.APPROVE)
        || event.getEvent().equals(NegotiationEvent.DECLINE)) {
      notificationService.statusChangeNotification(
          event.getNegotiationId(), event.getEvent(), event.getPost());
    }
  }

  @EventListener(value = NewNegotiationEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  @Async
  public void handleNewNegotiationEvent(NewNegotiationEvent event) {
    notificationService.createConfirmationNotification(event.getNegotiationId());
  }

  @EventListener(value = NegotiationStateChangeEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  @Async
  public void handleSubmittedNegotiation(NegotiationStateChangeEvent event) {
    log.error(Thread.currentThread().getName());
    if (event.getEvent().equals(NegotiationEvent.SUBMIT)) {
      try {
        notificationService.createConfirmationNotification(event.getNegotiationId());
      } catch (Exception e) {
        log.error("Error notifying researchers about negotiation submission", e);
      }
    }
  }
}
