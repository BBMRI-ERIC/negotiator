package eu.bbmri_eric.negotiator.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookSecretRepository extends JpaRepository<WebhookSecret, String> {}
