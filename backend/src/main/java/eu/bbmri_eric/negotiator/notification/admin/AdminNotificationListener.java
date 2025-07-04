package eu.bbmri_eric.negotiator.notification.admin;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import jakarta.transaction.Transactional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@CommonsLog
class AdminNotificationListener {

  AdminNotificationService adminNotificationService;

  AdminNotificationListener(AdminNotificationService adminNotificationService) {
    this.adminNotificationService = adminNotificationService;
  }

  @EventListener(
      value = NegotiationStateChangeEvent.class,
      condition = "event.changedTo.value == 'SUBMITTED'")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  @Async
  void onNewNegotiation(NegotiationStateChangeEvent event) {
    if (event.getEvent().equals(NegotiationEvent.SUBMIT)) {
      try {
        adminNotificationService.notifyAllAdmins(
            "New Request", "A new Request has been submitted for review", event.getNegotiationId());
      } catch (Exception e) {
        log.error("Error notifying admins about negotiation submission");
      }
    }
  }
}
