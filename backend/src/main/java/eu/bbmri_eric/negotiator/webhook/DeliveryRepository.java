package eu.bbmri_eric.negotiator.webhook;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, String> {
  Optional<Delivery> findByIdAndWebhookId(String id, Long webhookId);
}
