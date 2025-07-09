package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.negotiation.NewResourcesAddedEvent;
import jakarta.transaction.Transactional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class NewResourcesListener {

  @EventListener(value = NewResourcesAddedEvent.class)
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(Transactional.TxType.REQUIRES_NEW)
  public void onApplicationEvent(NewResourcesAddedEvent event) {
    // TODO: Fix
  }
}
