package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.database.model.*;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.repository.NotificationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.dto.NotificationDTO;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.json.JSONException;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@CommonsLog
@Transactional
public class UserNotificationServiceImpl implements UserNotificationService {

  @Autowired NotificationRepository notificationRepository;
  @Autowired PersonRepository personRepository;
  @Autowired ModelMapper modelMapper;
  @Autowired EmailService emailService;
  @Autowired ResourceLifecycleService resourceLifecycleService;
  @Autowired TemplateEngine templateEngine;
  @Autowired NegotiationRepository negotiationRepository;

  @Value("${negotiator.frontend-url}")
  private String frontendUrl;

  @Value("${reminder.trigger-duration-days:P7D}")
  private String triggerDuration;

  private static Set<Resource> getResourcesInNegotiationRepresentedBy(
      Negotiation negotiation, Person representative) {
    Set<Resource> overlappingResources = new HashSet<>(representative.getResources());
    overlappingResources.retainAll(negotiation.getResources());
    return overlappingResources;
  }

  private static Set<Person> getRepresentativesForNegotiation(Negotiation negotiation) {
    return negotiation.getResources().stream()
        .map(Resource::getRepresentatives)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public List<NotificationDTO> getNotificationsForUser(Long userId) {
    return notificationRepository.findByRecipientId(userId).stream()
        .map(notification -> modelMapper.map(notification, NotificationDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public void notifyAdmins(Negotiation negotiation) {
    List<Notification> newNotifications =
        createNotificationsForAdmins(
            negotiation,
            NotificationEmailStatus.EMAIL_SENT,
            "New Negotiation %s was added for review.");
    notificationRepository.saveAll(newNotifications);
    sendNotificationsToAdmins(newNotifications);
  }

  @Override
  public void notifyRepresentativesAboutNewNegotiation(Negotiation negotiation) {
    log.info("Notifying representatives about new negotiation.");
    createNotificationsForRepresentatives(negotiation);
    markResourcesWithoutARepresentative(negotiation);
  }

  @Override
  public void notifyRequesterAboutStatusChange(Negotiation negotiation, Resource resource) {
    log.info("Notifying researcher about status change.");
    notificationRepository.save(
        Notification.builder()
            .negotiation(negotiation)
            .emailStatus(NotificationEmailStatus.EMAIL_NOT_SENT)
            .recipient(negotiation.getCreatedBy())
            .message(
                "Negotiation %s had a change of status of %s to %s"
                    .formatted(
                        negotiation.getId(),
                        resource.getSourceId(),
                        negotiation.getCurrentStatePerResource().get(resource.getSourceId())))
            .build());
  }

  @Override
  @Transactional
  public void notifyUsersAboutNewPost(Post post) {
    log.info("Notifying users about new post.");
    if (!postAuthorIsAlsoRequester(post)) {
      createNotificationForRequester(post);
    }
    if (post.isPublic()) {
      createNotificationsForRepresentatives(post);
    } else if (!post.isPublic() && Objects.nonNull(post.getOrganization())) {
      createNotificationsForPrivatePost(post);
    }
  }

  private void createNotificationsForPrivatePost(Post post) {
    Set<Person> representatives = getRepresentativesOfOrganization(post);
    log.info(representatives.size());
    for (Person representative : representatives) {
      if (!representative.getId().equals(post.getCreatedBy().getId())) {
        notificationRepository.save(
            Notification.builder()
                .negotiation(post.getNegotiation())
                .emailStatus(NotificationEmailStatus.EMAIL_NOT_SENT)
                .recipient(representative)
                .message(
                    "Negotiation %s had a new post by %s"
                        .formatted(post.getNegotiation().getId(), post.getCreatedBy().getName()))
                .build());
      }
    }
  }

  @NonNull
  private static Set<Person> getRepresentativesOfOrganization(Post post) {
    Set<Person> representatives = new HashSet<>(Set.of());
    post.getOrganization()
        .getResources()
        .forEach(resource -> representatives.addAll(resource.getRepresentatives()));
    return representatives;
  }

  private void createNotificationsForRepresentatives(Post post) {
    for (Person representative : getRepresentativesForNegotiation(post.getNegotiation())) {
      if (!representative.getId().equals(post.getCreatedBy().getId())) {
        notificationRepository.save(
            Notification.builder()
                .negotiation(post.getNegotiation())
                .emailStatus(NotificationEmailStatus.EMAIL_NOT_SENT)
                .recipient(representative)
                .message(
                    "Negotiation %s had a new post by %s"
                        .formatted(post.getNegotiation().getId(), post.getCreatedBy().getName()))
                .build());
      }
    }
  }

  private void createNotificationForRequester(Post post) {
    notificationRepository.save(
        Notification.builder()
            .negotiation(post.getNegotiation())
            .emailStatus(NotificationEmailStatus.EMAIL_NOT_SENT)
            .recipient(post.getNegotiation().getCreatedBy())
            .message(
                "Negotiation %s had a new post by %s"
                    .formatted(post.getNegotiation().getId(), post.getCreatedBy().getName()))
            .build());
  }

  private static boolean postAuthorIsAlsoRequester(Post post) {
    return post.getCreatedBy().equals(post.getNegotiation().getCreatedBy());
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

  private List<Notification> createNotificationsForAdmins(
      Negotiation negotiation, NotificationEmailStatus status, String messageTemplate) {
    List<Notification> newNotifications = new ArrayList<>();
    for (Person admin : personRepository.findAllByAdminIsTrue()) {
      Notification newNotification =
          buildNewNotification(
              negotiation, status, admin, String.format(messageTemplate, negotiation.getId()));
      newNotifications.add(newNotification);
    }
    return newNotifications;
  }

  private void sendNotificationsToAdmins(List<Notification> notifications) {
    for (Notification notification : notifications) {
      sendEmail(notification.getRecipient(), Collections.singletonList(notification));
    }
  }

  private void markResourcesWithoutARepresentative(@NonNull Negotiation negotiation) {
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

  private void markReachableResources(
      Negotiation negotiation, @NonNull Set<Resource> overlappingResources) {
    for (Resource resourceWithRepresentative : overlappingResources) {
      resourceLifecycleService.sendEvent(
          negotiation.getId(),
          resourceWithRepresentative.getSourceId(),
          NegotiationResourceEvent.CONTACT);
    }
  }

  private void createNewNotification(
      Negotiation negotiation, NotificationEmailStatus emailNotSent, Person representative) {
    notificationRepository.save(
        buildNewNotification(
            negotiation,
            emailNotSent,
            representative,
            "New Negotiation %s ".formatted(negotiation.getId())));
  }

  private Notification buildNewNotification(
      Negotiation negotiation,
      NotificationEmailStatus emailNotSent,
      Person representative,
      String message) {
    Notification new_notification =
        Notification.builder()
            .negotiation(negotiation)
            .emailStatus(emailNotSent)
            .recipient(representative)
            .message(message)
            .build();
    return new_notification;
  }

  @Override
  @Scheduled(cron = "${notification.cron-schedule-expression:0 0 * * * *}")
  @Async
  public void sendEmailsForNewNotifications() {
    log.info("Sending new email notifications.");
    Set<Person> recipients = getPendingRecipients();
    sendOutNotificationEmails(recipients);
  }

  private void sendOutNotificationEmails(@NonNull Set<Person> recipients) {
    for (Person recipient : recipients) {
      List<Notification> notifications = getPendingNotifications(recipient);
      sendEmail(recipient, notifications);
      markNotificationsAsEmailSent(notifications);
    }
  }

  private void markNotificationsAsEmailSent(@NonNull List<Notification> notifications) {
    for (Notification notification : notifications) {
      notification.setEmailStatus(NotificationEmailStatus.EMAIL_SENT);
      notificationRepository.save(notification);
    }
  }

  private void sendEmail(@NonNull Person recipient, @NonNull List<Notification> notifications) {
    sendEmail(recipient, notifications, "email-notification");
  }

  private void sendReminderEmail(
      @NonNull Person recipient, @NonNull List<Notification> notifications) {
    sendEmail(recipient, notifications, "email-reminder-notification");
  }

  private void sendEmail(
      @NonNull Person recipient, @NonNull List<Notification> notifications, String email_template) {

    Context context = new Context();
    List<Negotiation> negotiations =
        notifications.stream()
            .map(notification -> notification.getNegotiation())
            .distinct()
            .collect(Collectors.toList());

    Map<String, String> roleForNegotiation = populateRoleForNegotiationMap(notifications);
    Map<String, String> titleForNegotiation = populateTitleForNegotiationMap(notifications);

    context.setVariable("negotiations", negotiations);
    context.setVariable("frontendurl", frontendUrl);
    context.setVariable("roleForNegotiation", roleForNegotiation);
    context.setVariable("titleForNegotiation", titleForNegotiation);

    String emailContent = templateEngine.process(email_template, context);

    emailService.sendEmail(recipient, "New Notifications", emailContent);
  }

  private Map<String, String> populateRoleForNegotiationMap(List<Notification> notifications) {
    Map<String, String> roleForNegotiation = new HashMap<>();
    for (Notification notification : notifications) {
      String negotiationId = notification.getNegotiation().getId();
      String role = extractRoleFromNotificationMessage(notification);
      roleForNegotiation.put(negotiationId, role);
    }
    return roleForNegotiation;
  }

  private Map<String, String> populateTitleForNegotiationMap(List<Notification> notifications) {
    Map<String, String> titleForNegotiation = new HashMap<>();
    for (Notification notification : notifications) {
      Negotiation negotiation = notification.getNegotiation();
      String negotiatorId = negotiation.getId();
      String title = parseTitleFromNegotiation(negotiation);
      titleForNegotiation.put(negotiatorId, title);
    }
    return titleForNegotiation;
  }

  private static String parseTitleFromNegotiation(Negotiation negotiation) {
    String title;
    try {
      JSONObject payloadJson = new JSONObject(negotiation.getPayload());
      title = payloadJson.getJSONObject("project").getString("title");
    } catch (JSONException e) {
      log.error("Failed to extract title from payload JSON", e);
      title = "Untitled negotiation";
    }
    return title;
  }

  private String extractRoleFromNotificationMessage(Notification notification) {
    String message = notification.getMessage();
    if (message.matches("New Negotiation .* was added for review\\.")) {
      return "ROLE_ADMIN";
    } else if (message.matches("Negotiation .* had a change of status of .* to .*")) {
      // TODO if status changed to "ACCESS_CONDITIONS_MET" role should be "ROLE_REPRESENTATIVE"
      // (once notification also goes to REPRESENTATIVE)
      return "ROLE_RESEARCHER";
    } else if (message.matches("Negotiation .* had a new post by .*")) {
      String[] parts = message.split("new post by");
      String negotiationCreator = notification.getNegotiation().getCreatedBy().getName();
      String postCreator = parts[1].trim();
      return (negotiationCreator.equals(postCreator)) ? "ROLE_REPRESENTATIVE" : "ROLE_RESEARCHER";
    } else if (message.matches("New Negotiation .*")) {
      return "ROLE_REPRESENTATIVE";
    } else {
      return "ROLE_RESEARCHER";
    }
  }

  private List<Notification> getPendingNotifications(@NonNull Person recipient) {
    return notificationRepository.findByRecipientIdAndEmailStatus(
        recipient.getId(), NotificationEmailStatus.EMAIL_NOT_SENT);
  }

  private Set<Person> getPendingRecipients() {
    return notificationRepository.findByEmailStatus(NotificationEmailStatus.EMAIL_NOT_SENT).stream()
        .map(Notification::getRecipient)
        .collect(Collectors.toSet());
  }

  private List<Notification> remindAdmins(Negotiation negotiation) {
    List<Notification> newNotifications =
        createNotificationsForAdmins(
            negotiation,
            NotificationEmailStatus.EMAIL_SENT,
            "The negotiation (%s) is pending review.");
    notificationRepository.saveAll(newNotifications);
    return newNotifications;
  }

  @Scheduled(cron = "${reminder.cron-schedule-expression:0 0 8 * * *}")
  public void sendRemindersEveryMorning() {
    sendReminderForPendingNegotiations();
  }

  public void sendReminderForPendingNegotiations() {
    Duration durationThreshold = Duration.parse(triggerDuration);
    LocalDateTime thresholdTime = LocalDateTime.now().minus(durationThreshold);

    List<Negotiation> pendingNegotiations =
        negotiationRepository.findByModifiedDateBeforeAndCurrentState(
            thresholdTime, NegotiationState.SUBMITTED);
    List<Notification> reminderNotifications = new ArrayList<>();

    for (Negotiation negotiation : pendingNegotiations) {
      reminderNotifications.addAll(remindAdmins(negotiation));
    }

    for (Person admin : personRepository.findAllByAdminIsTrue()) {
      sendReminderEmail(admin, reminderNotifications);
    }
  }
}
