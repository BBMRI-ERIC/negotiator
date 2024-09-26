package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.email.EmailService;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.apachecommons.CommonsLog;
import org.json.JSONException;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
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

  NotificationRepository notificationRepository;
  PersonRepository personRepository;
  ModelMapper modelMapper;
  EmailService emailService;
  TemplateEngine templateEngine;
  NegotiationRepository negotiationRepository;

  @Value("${negotiator.frontend-url}")
  private String frontendUrl;

  @Value("${reminder.trigger-duration-days:P7D}")
  private String triggerDuration;

  @Value("${negotiator.emailYoursSincerelyText}")
  private String emailYoursSincerelyText;

  @Value("${negotiator.emailHelpdeskHref}")
  private String emailHelpdeskHref;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  private static LocalDateTime thresholdTime;

  private static Collection<NegotiationResourceState> nonactiveNegotiationResourceStates =
      Arrays.asList(
          NegotiationResourceState.RESOURCE_UNAVAILABLE,
          NegotiationResourceState.REPRESENTATIVE_UNREACHABLE,
          NegotiationResourceState.RESOURCE_MADE_AVAILABLE,
          NegotiationResourceState.RESOURCE_NOT_MADE_AVAILABLE);

  public UserNotificationServiceImpl(
      NotificationRepository notificationRepository,
      PersonRepository personRepository,
      ModelMapper modelMapper,
      EmailService emailService,
      TemplateEngine templateEngine,
      NegotiationRepository negotiationRepository) {
    this.notificationRepository = notificationRepository;
    this.personRepository = personRepository;
    this.modelMapper = modelMapper;
    this.emailService = emailService;
    this.templateEngine = templateEngine;
    this.negotiationRepository = negotiationRepository;
  }

  private static Set<Resource> getResourcesInNegotiationRepresentedBy(
      Negotiation negotiation, Person representative) {
    Set<Resource> overlappingResources = new HashSet<>(representative.getResources());
    overlappingResources.retainAll(negotiation.getResources());
    return overlappingResources;
  }

  private static Set<Person> getRepresentativesForNegotiation(Negotiation negotiation) {
    return negotiation.getResources().stream()
        .filter(
            resource ->
                Objects.equals(
                    negotiation.getCurrentStateForResource(resource.getSourceId()),
                    NegotiationResourceState.SUBMITTED))
        .map(Resource::getRepresentatives)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private static Set<Person> getRepresentativesForStaleNegotiation(Negotiation negotiation) {

    Map<Resource, Set<NegotiationResourceLifecycleRecord>> recordsByResource =
        negotiation.getNegotiationResourceLifecycleRecords().stream()
            .collect(
                Collectors.groupingBy(
                    NegotiationResourceLifecycleRecord::getResource, Collectors.toSet()));

    Set<Resource> resourcesWithoutRecentRecord =
        recordsByResource.entrySet().stream()
            .filter(
                entry ->
                    entry.getValue().stream()
                        .noneMatch(
                            record ->
                                record.getModifiedDate().isAfter(thresholdTime)
                                    || nonactiveNegotiationResourceStates.contains(
                                        record.getChangedTo())))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

    Set<Person> representatives =
        resourcesWithoutRecentRecord.stream()
            .map(Resource::getRepresentatives)
            .flatMap(Set::stream)
            .collect(Collectors.toSet());

    return representatives;
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
            "New Negotiation %s was added for review.".formatted(negotiation.getId()));
    notificationRepository.saveAll(newNotifications);
    sendNotificationsToAdmins(
        newNotifications.stream()
            .map(
                (notification) ->
                    new NotificationViewDTO(
                        notification.getId(),
                        notification.getMessage(),
                        notification.getEmailStatus(),
                        negotiation.getId(),
                        parseTitleFromNegotiation(negotiation),
                        notification.getRecipient()))
            .collect(Collectors.toList()));
  }

  @Override
  public void notifyRepresentativesAboutNewNegotiation(Negotiation negotiation) {
    log.info("Notifying representatives about new negotiation.");
    createNotificationsForRepresentatives(negotiation);
    markResourcesWithoutARepresentative(negotiation);
  }

  @Override
  public void notifyRepresentativesAboutNewNegotiation(String negotiationId) {
    Negotiation negotiation =
        negotiationRepository
            .findById(negotiationId)
            .orElseThrow(() -> new EntityNotFoundException(negotiationId));
    notifyRepresentativesAboutNewNegotiation(negotiation);
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
                        negotiation.getCurrentStateForResource(resource.getSourceId())))
            .build());
  }

  @Override
  @Transactional
  public void notifyUsersAboutNewPost(Post post) {
    log.info("Notifying users about new post.");
    if (!postAuthorIsAlsoRequester(post)) {
      createNotificationForRequester(post);
    }
    if (post.isPublic()
        && Objects.equals(post.getNegotiation().getCurrentState(), NegotiationState.IN_PROGRESS)) {
      createNotificationsForRepresentatives(post);
    } else if (!post.isPublic() && Objects.nonNull(post.getOrganization())) {
      createNotificationsForPrivatePost(post);
    }
  }

  private void createNotificationsForPrivatePost(Post post) {
    Set<Person> representatives = getRepresentativesOfOrganization(post);
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
      createNewNotification(negotiation, representative);
      Set<Resource> overlappingResources =
          getResourcesInNegotiationRepresentedBy(negotiation, representative);
      markReachableResources(negotiation, overlappingResources);
    }
  }

  private List<Notification> createNotificationsForRepresentatives(
      Negotiation negotiation, NotificationEmailStatus emailStatus, String emailMessage) {
    Set<Person> representatives = getRepresentativesForStaleNegotiation(negotiation);
    List<Notification> newNotifications = new ArrayList<>();
    for (Person representative : representatives) {
      Notification reminderNotification =
          buildNewNotification(negotiation, emailStatus, representative, emailMessage);
      newNotifications.add(reminderNotification);
    }
    return newNotifications;
  }

  private List<Notification> createNotificationsForAdmins(
      Negotiation negotiation, NotificationEmailStatus status, String notificationMessage) {
    List<Notification> newNotifications = new ArrayList<>();
    for (Person admin : personRepository.findAllByAdminIsTrue()) {
      Notification newNotification =
          buildNewNotification(negotiation, status, admin, notificationMessage);
      newNotifications.add(newNotification);
    }
    return newNotifications;
  }

  private void sendNotificationsToAdmins(List<NotificationViewDTO> notifications) {
    for (NotificationViewDTO notification : notifications) {
      sendEmail(notification.getRecipient(), Collections.singletonList(notification));
    }
  }

  private void markResourcesWithoutARepresentative(@NonNull Negotiation negotiation) {
    for (Resource resourceWithoutRep :
        negotiation.getResources().stream()
            .filter(resource -> resource.getRepresentatives().isEmpty())
            .collect(Collectors.toSet())) {
      log.warn(
          "Resource with ID: %s does not have a representative."
              .formatted(resourceWithoutRep.getSourceId()));
      negotiation.setStateForResource(
          resourceWithoutRep.getSourceId(), NegotiationResourceState.REPRESENTATIVE_UNREACHABLE);
    }
  }

  private void markReachableResources(
      Negotiation negotiation, @NonNull Set<Resource> overlappingResources) {
    for (Resource resourceWithRepresentative : overlappingResources) {
      negotiation.setStateForResource(
          resourceWithRepresentative.getSourceId(),
          NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    }
  }

  private void createNewNotification(Negotiation negotiation, Person representative) {
    notificationRepository.save(
        buildNewNotification(
            negotiation,
            NotificationEmailStatus.EMAIL_NOT_SENT,
            representative,
            "New Negotiation %s ".formatted(negotiation.getId())));
  }

  private Notification buildNewNotification(
      Negotiation negotiation,
      NotificationEmailStatus emailNotSent,
      Person representative,
      String message) {
    Notification newNotification =
        Notification.builder()
            .negotiation(negotiation)
            .emailStatus(emailNotSent)
            .recipient(representative)
            .message(message)
            .build();
    newNotification.setModifiedDate(LocalDateTime.now());
    newNotification.setCreationDate(LocalDateTime.now());
    return newNotification;
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
      List<NotificationViewDTO> notifications = getPendingNotifications(recipient);
      sendEmail(recipient, notifications);
      markNotificationsAsEmailSent(notifications);
    }
  }

  private void markNotificationsAsEmailSent(@NonNull List<NotificationViewDTO> notifications) {
    for (NotificationViewDTO notificationView : notifications) {
      Notification notification =
          notificationRepository.findById(notificationView.getId()).orElseThrow();
      notification.setEmailStatus(NotificationEmailStatus.EMAIL_SENT);
      notification.setModifiedDate(LocalDateTime.now());
      notificationRepository.save(notification);
    }
  }

  private void sendEmail(
      @NonNull Person recipient, @NonNull List<NotificationViewDTO> notifications) {
    sendEmail(recipient, notifications, "email-notification");
  }

  private void sendEmail(
      @NonNull Person recipient,
      @NonNull List<NotificationViewDTO> notifications,
      String emailTemplate) {

    Context context = new Context();
    List<String> negotiationsIds =
        notifications.stream()
            .map(NotificationViewDTO::getNegotiationId)
            .distinct()
            .collect(Collectors.toList());

    Map<String, String> roleForNegotiation = populateRoleForNegotiationMap(notifications);
    Map<String, String> titleForNegotiation = populateTitleForNegotiationMap(notifications);
    Map<String, List<NotificationViewDTO>> notificationsForNegotiation =
        notifications.stream()
            .collect(Collectors.groupingBy(NotificationViewDTO::getNegotiationId));

    context.setVariable("recipient", recipient);
    context.setVariable("negotiations", negotiationsIds);
    context.setVariable("frontendUrl", frontendUrl);
    context.setVariable("roleForNegotiation", roleForNegotiation);
    context.setVariable("titleForNegotiation", titleForNegotiation);
    context.setVariable("notificationsForNegotiation", notificationsForNegotiation);
    context.setVariable("emailYoursSincerelyText", emailYoursSincerelyText);
    context.setVariable("emailHelpdeskHref", emailHelpdeskHref);
    context.setVariable("logoUrl", logoURL);

    String emailContent = templateEngine.process(emailTemplate, context);

    emailService.sendEmail(recipient, "New Notifications", emailContent);
  }

  private Map<String, String> populateRoleForNegotiationMap(
      List<NotificationViewDTO> notifications) {
    Map<String, String> roleForNegotiation = new HashMap<>();
    for (NotificationViewDTO notification : notifications) {
      String negotiationId = notification.getNegotiationId();
      String role = extractRole(notification);
      roleForNegotiation.put(negotiationId, role);
    }
    return roleForNegotiation;
  }

  private Map<String, String> populateTitleForNegotiationMap(
      List<NotificationViewDTO> notifications) {
    Map<String, String> titleForNegotiation = new HashMap<>();
    for (NotificationViewDTO notification : notifications) {
      String negotiatorId = notification.getNegotiationId();
      String title = notification.getNegotiationTitle();
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

  private String extractRole(NotificationViewDTO notification) {
    String message = notification.getMessage();
    if (message.matches("New Negotiation .* was added for review\\.")
        || message.matches("The negotiation .* is awaiting review\\.")) {
      return "ROLE_ADMIN";
    } else if (personRepository.isNegotiationCreator(
        notification.getRecipient().getId(), notification.getNegotiationId())) {
      return "ROLE_RESEARCHER";
    } else {
      return "ROLE_REPRESENTATIVE";
    }
  }

  private List<NotificationViewDTO> getPendingNotifications(@NonNull Person recipient) {
    return notificationRepository.findViewByRecipientIdAndEmailStatus(
        recipient.getId(), NotificationEmailStatus.EMAIL_NOT_SENT);
  }

  private Set<Person> getPendingRecipients() {
    return notificationRepository.findByEmailStatus(NotificationEmailStatus.EMAIL_NOT_SENT).stream()
        .map(Notification::getRecipient)
        .collect(Collectors.toSet());
  }

  private void remindAdmins(Negotiation negotiation) {
    List<Notification> newNotifications =
        createNotificationsForAdmins(
            negotiation,
            NotificationEmailStatus.EMAIL_NOT_SENT,
            "The negotiation %s is awaiting review.".formatted(negotiation.getId()));
    notificationRepository.saveAll(newNotifications);
  }

  private void remindRepresentatives(Negotiation negotiation) {
    List<Notification> reminderNotifications =
        createNotificationsForRepresentatives(
            negotiation,
            NotificationEmailStatus.EMAIL_NOT_SENT,
            "The negotiation %s is stale and had no status change in a while."
                .formatted(negotiation.getId()));
    notificationRepository.saveAll(reminderNotifications);
  }

  @Scheduled(cron = "${reminder.cron-schedule-expression:0 0 0 * * MON-FRI}")
  @Async
  public void createRemindersOldNegotiations() {
    log.info("Creating reminder email notifications.");
    Duration durationThreshold = Duration.parse(triggerDuration);
    thresholdTime = LocalDateTime.now().minus(durationThreshold);

    createReminderForPendingNegotiations(thresholdTime);
    createReminderForStaleNegotiations(thresholdTime);
  }

  private void createReminderForPendingNegotiations(LocalDateTime thresholdTime) {
    log.info("Creating reminder email for pending notifications.");
    List<Negotiation> pendingNegotiations =
        negotiationRepository.findByModifiedDateBeforeAndCurrentState(
            thresholdTime, NegotiationState.SUBMITTED);
    for (Negotiation negotiation : pendingNegotiations) {
      remindAdmins(negotiation);
    }
  }

  private void createReminderForStaleNegotiations(LocalDateTime thresholdTime) {
    log.info("Creating reminder email for stale notifications.");

    List<Negotiation> inProgressNegotiations =
        negotiationRepository.findByModifiedDateBeforeAndCurrentState(
            thresholdTime, NegotiationState.IN_PROGRESS);

    for (Negotiation negotiation : inProgressNegotiations) {
      remindRepresentatives(negotiation);
    }
  }
}
