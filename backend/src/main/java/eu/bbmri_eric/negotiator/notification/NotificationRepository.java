package eu.bbmri_eric.negotiator.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository
    extends JpaRepository<Notification, Long>, PagingAndSortingRepository<Notification, Long> {

  List<Notification> findByRecipientId(Long personId);

  List<Notification> findAllByRecipientId(Long personId);

  Page<Notification> findAllByRecipientId(Long personId, Pageable pageable);

  @Query(
      value =
          "SELECT nt.id, nt.message, ng.id, ng.title, nt.recipient_id "
              + "FROM notification nt "
              + "JOIN negotiation ng ON nt.negotiation_id = ng.id "
              + "WHERE nt.recipient_id = :recipientId",
      nativeQuery = true)
  List<Object[]> findViewByRecipientId(@Param("recipientId") String recipientId);
}
