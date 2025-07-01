package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface NotificationRepository
    extends JpaRepository<Notification, Long>, PagingAndSortingRepository<Notification, Long> {

  List<Notification> findByRecipientId(Long personId);

  List<Notification> findByEmailStatus(NotificationEmailStatus status);

  List<Notification> findAllByRecipient_id(Long personId);

  Page<Notification> findAllByRecipient_id(Long personId, Pageable pageable);

  @Query(
      "SELECT new eu.bbmri_eric.negotiator.notification.NotificationViewDTO("
          + "nt.id, nt.message, nt.emailStatus, ng.id, ng.title, p) "
          + "FROM Notification nt "
          + "JOIN nt.negotiation ng "
          + "JOIN nt.recipient p "
          + "WHERE p.id = :recipientId AND "
          + "nt.emailStatus = :status")
  List<NotificationViewDTO> findViewByRecipientIdAndEmailStatus(
      Long recipientId, NotificationEmailStatus status);
}
