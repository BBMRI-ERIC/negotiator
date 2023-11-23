package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Notification;
import eu.bbmri.eric.csit.service.negotiator.database.model.NotificationEmailStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
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
  @Autowired EmailService emailService;
  @Autowired ResourceLifecycleService resourceLifecycleService;

  @Override
  public List<NotificationDTO> getNotificationsForUser(Long userId) {
    return notificationRepository.findByRecipientId(userId).stream()
        .map(notification -> modelMapper.map(notification, NotificationDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public void notifyAdmins(Negotiation negotiation) {
    for (Person admin : personRepository.findAllByAdminIsTrue()) {
      createNewNotification(negotiation, NotificationEmailStatus.EMAIL_SENT, admin);
      emailService.sendEmail(
          admin.getAuthEmail(), "New Negotiation", "New Negotiation was added for review.");
    }
  }

  @Override
  public void notifyRepresentativesAboutNewNegotiation(Negotiation negotiation) {
    log.info("Notifying representatives about new negotiation.");
    createNotificationsForRepresentatives(negotiation);
    markResourcesWithoutARepresentative(negotiation);
  }

  private void createNotificationsForRepresentatives(Negotiation negotiation) {
    Set<Person> representatives = getRepresentativesForNegotiation(negotiation);
    for (Person representative : representatives) {
      createNewNotification(negotiation, NotificationEmailStatus.EMAIL_NOT_SENT, representative);
      Set<Resource> overlappingResources =
          getResourcesInNegotiationRepresentedBy(negotiation, representative);
      markReachableResources(negotiation, overlappingResources);
    }
  }

  private void markResourcesWithoutARepresentative(Negotiation negotiation) {
    for (Resource resourceWithoutRep :
        negotiation.getResources().stream()
            .filter(resource -> resource.getRepresentatives().isEmpty())
            .collect(Collectors.toSet())) {
      log.info(resourceWithoutRep.getSourceId());
      resourceLifecycleService.sendEvent(
          negotiation.getId(),
          resourceWithoutRep.getSourceId(),
          NegotiationResourceEvent.MARK_AS_UNREACHABLE);
    }
  }

  private void markReachableResources(Negotiation negotiation, Set<Resource> overlappingResources) {
    for (Resource resourceWithRepresentative : overlappingResources) {
      resourceLifecycleService.sendEvent(
          negotiation.getId(),
          resourceWithRepresentative.getSourceId(),
          NegotiationResourceEvent.CONTACT);
    }
  }

  private static Set<Resource> getResourcesInNegotiationRepresentedBy(
      Negotiation negotiation, Person representative) {
    Set<Resource> overlappingResources = representative.getResources();
    overlappingResources.retainAll(negotiation.getResources());
    return overlappingResources;
  }

  private void createNewNotification(
      Negotiation negotiation, NotificationEmailStatus emailNotSent, Person representative) {
    notificationRepository.save(
        Notification.builder()
            .negotiation(negotiation)
            .emailStatus(emailNotSent)
            .recipient(representative)
            .message("New")
            .build());
  }

  private static Set<Person> getRepresentativesForNegotiation(Negotiation negotiation) {
    return negotiation.getResources().stream()
        .map(Resource::getRepresentatives)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  @Scheduled(cron = "0 0 * * * *")
  @Async
  public void sendEmailsForNewNotifications() {
    log.info("Sending new email notifications.");
    Set<Person> recipients = getPendingRecipients();
    sendOutNotificationEmails(recipients);
  }

  private void sendOutNotificationEmails(Set<Person> recipients) {
    for (Person recipient : recipients) {
      List<Notification> notifications = getPendingNotifications(recipient);
      sendEmail(recipient, notifications);
      markNotificationsAsEmailSent(notifications);
    }
  }

  private void markNotificationsAsEmailSent(List<Notification> notifications) {
    for (Notification notification : notifications) {
      notification.setEmailStatus(NotificationEmailStatus.EMAIL_SENT);
      notificationRepository.save(notification);
    }
  }

  private void sendEmail(Person recipient, List<Notification> notifications) {
    emailService.sendEmail(
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

  private List<Notification> getPendingNotifications(Person recipient) {
    return notificationRepository.findByRecipientIdAndEmailStatus(
        recipient.getId(), NotificationEmailStatus.EMAIL_NOT_SENT);
  }

  private Set<Person> getPendingRecipients() {
    return notificationRepository.findByEmailStatus(NotificationEmailStatus.EMAIL_NOT_SENT).stream()
        .map(Notification::getRecipient)
        .collect(Collectors.toSet());
  }
}
