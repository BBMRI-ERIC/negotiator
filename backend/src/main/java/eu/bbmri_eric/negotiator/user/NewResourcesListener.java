package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import eu.bbmri_eric.negotiator.notification.OldNotificationService;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NewResourcesListener {
  private final OldNotificationService notificationService;

  public NewResourcesListener(OldNotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @EventListener(value = NewResourcesAddedEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onApplicationEvent(NewResourcesAddedEvent event) {
    notificationService.notifyRepresentativesAboutNewNegotiation(event.getNegotiationId());
  }
}
