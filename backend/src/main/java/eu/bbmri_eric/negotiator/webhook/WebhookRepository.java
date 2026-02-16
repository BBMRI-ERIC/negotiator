package eu.bbmri_eric.negotiator.webhook;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookRepository extends JpaRepository<Webhook, Long> {
  List<Webhook> findByActiveTrue();
}
