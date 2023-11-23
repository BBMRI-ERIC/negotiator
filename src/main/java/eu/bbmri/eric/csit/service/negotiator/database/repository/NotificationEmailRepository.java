package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationEmailRepository extends JpaRepository<NotificationEmail, Long> {}
