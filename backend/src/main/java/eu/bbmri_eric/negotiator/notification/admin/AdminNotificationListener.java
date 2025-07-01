package eu.bbmri_eric.negotiator.notification.admin;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.OldNotificationService;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@CommonsLog
public class AdminNotificationListener {

  private final OldNotificationService oldNotificationService;

  public AdminNotificationListener(OldNotificationService oldNotificationService) {
    this.oldNotificationService = oldNotificationService;
  }

  @EventListener(value = NegotiationStateChangeEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  @Async
  public void handleSubmittedNegotiation(NegotiationStateChangeEvent event) {
    if (event.getEvent().equals(NegotiationEvent.SUBMIT)) {
      try {
        oldNotificationService.notifyAdmins(event.getNegotiationId());
      } catch (Exception e) {
        log.error("Error notifying admins about negotiation submission");
      }
    }
  }
}
