package eu.bbmri_eric.negotiator.notification;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

interface NotificationRepository
    extends JpaRepository<Notification, Long>, PagingAndSortingRepository<Notification, Long> {

  List<Notification> findByRecipientId(Long personId);

  List<Notification> findAllByRecipientId(Long personId);

  Page<Notification> findAllByRecipientId(Long personId, Pageable pageable);
}
