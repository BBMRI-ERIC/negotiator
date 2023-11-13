package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmailStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NotificationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.NotificationDTO;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class UserNotificationServiceImpl implements UserNotificationService {

  @Autowired NotificationRepository notificationRepository;
  @Autowired PersonRepository personRepository;
  @Autowired ModelMapper modelMapper;
  @Autowired NotificationService notificationService;

  @Override
  public List<NotificationDTO> getNotificationsForUser(Long userId) {
    return notificationRepository.findByRecipientId(userId).stream()
        .map(notification -> modelMapper.map(notification, NotificationDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public void notifyAdmins(Negotiation negotiation) {
    for (Person admin : personRepository.findAllByAdminIsTrue()) {
      notificationRepository.save(
          Notification.builder()
              .negotiation(negotiation)
              .emailStatus(NotificationEmailStatus.EMAIL_SENT)
              .recipient(admin)
              .message("New")
              .build());
      notificationService.sendEmail(
          admin.getAuthEmail(), "New Negotiation", "New Negotiation was added for review.");
    }
  }

  @Override
  @Scheduled(cron = "0 0 * * * *")
  @Async
  public void notifyUsersAboutNewNotifications() {
    log.info("Sending new email notifications.");
    Set<Person> recipients =
        notificationRepository.findByEmailStatus(NotificationEmailStatus.EMAIL_NOT_SENT).stream()
            .map(Notification::getRecipient)
            .collect(Collectors.toSet());
    for (Person recipient : recipients) {
      List<Notification> notifications =
          notificationRepository.findByRecipientIdAndEmailStatus(
              recipient.getId(), NotificationEmailStatus.EMAIL_NOT_SENT);
      notificationService.sendEmail(
          recipient.getAuthEmail(),
          "New Notifications",
          "There are updates in the following negotiations "
              + String.join(
                  ",",
                  notifications.stream()
                      .map(Notification::getNegotiation)
                      .map(Negotiation::getId)
                      .collect(Collectors.toSet()))
              + " new notifications.");
    }
  }
}
