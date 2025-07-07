package eu.bbmri_eric.negotiator.notification.representative;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.RepresentativeNotificationService;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
@Component
class RepresentativeNotificationListener {

    RepresentativeNotificationService representativeNotificationService;

    RepresentativeNotificationListener(RepresentativeNotificationService representativeNotificationService) {
        this.representativeNotificationService = representativeNotificationService;
    }

    @EventListener(
            value = NegotiationStateChangeEvent.class,
            condition = "event.changedTo.value == 'IN_PROGRESS'")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @Async
    void onApprovedNegotiation(NegotiationStateChangeEvent event) {
        representativeNotificationService.notifyAboutANewNegotiation(event.getNegotiationId());
    }
}
