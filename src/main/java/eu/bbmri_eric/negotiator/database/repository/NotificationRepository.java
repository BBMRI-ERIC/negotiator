package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Notification;
import eu.bbmri_eric.negotiator.database.model.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.database.model.views.NotificationViewDTO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  List<Notification> findByRecipientId(Long personId);

  List<Notification> findByEmailStatus(NotificationEmailStatus status);

  @Query(
      "SELECT new eu.bbmri_eric.negotiator.database.model.views.NotificationViewDTO("
          + "nt.id, nt.message, nt.emailStatus, ng.id, ng.title, p) "
          + "FROM Notification nt "
          + "JOIN nt.negotiation ng "
          + "JOIN nt.recipient p "
          + "WHERE p.id = :recipientId AND "
          + "nt.emailStatus = :status")
  List<NotificationViewDTO> findViewByRecipientIdAndEmailStatus(
      Long recipientId, NotificationEmailStatus status);
}
