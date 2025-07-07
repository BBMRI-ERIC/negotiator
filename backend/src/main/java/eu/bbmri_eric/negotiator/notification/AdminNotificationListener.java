package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.negotiation.NewNegotiationEvent;
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
    void onSubmittedNegotiation(NegotiationStateChangeEvent event) {
        notifyAdministratorsAboutANewNegotiation(event.getNegotiationId());
    }

    @EventListener(NewNegotiationEvent.class)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Async
    void onNewNegotiation(NewNegotiationEvent event) {
        notifyAdministratorsAboutANewNegotiation(event.getNegotiationId());
    }

    private void notifyAdministratorsAboutANewNegotiation(String negotiationId) {
        try {
            adminNotificationService.notifyAllAdmins(
                    "New Request", "A new Request has been submitted for review", negotiationId);
        } catch (Exception e) {
            log.error("Error notifying admins about negotiation submission");
        }
    }
}
