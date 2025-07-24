package eu.bbmri_eric.negotiator.email;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationEmailRepository
    extends JpaRepository<NotificationEmail, Long>, JpaSpecificationExecutor<NotificationEmail> {}
