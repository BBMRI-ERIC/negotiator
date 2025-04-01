package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.notification.email.EmailService;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
import org.jsoup.Jsoup;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
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
  ResourceLoader resourceLoader;

  @Value("${negotiator.frontend-url}")
  private String frontendUrl;

  @Value("${negotiator.emailYoursSincerelyText}")
  private String emailYoursSincerelyText;

  @Value("${negotiator.emailHelpdeskHref}")
  private String emailHelpdeskHref;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  @Value("${spring.thymeleaf.prefix:classpath:/templates/}")
  private String thymeleafPrefix;

  @Value("${spring.thymeleaf.suffix:.html}")
  private String thymeleafSuffix;

  private final String defaultThymeleafPrefix = "classpath:/templates/";

  public UserNotificationServiceImpl(
      NotificationRepository notificationRepository,
      PersonRepository personRepository,
      ModelMapper modelMapper,
      EmailService emailService,
      TemplateEngine templateEngine,
      NegotiationRepository negotiationRepository,
      ResourceLoader resourceLoader) {
    this.notificationRepository = notificationRepository;
    this.personRepository = personRepository;
    this.modelMapper = modelMapper;
    this.emailService = emailService;
    this.templateEngine = templateEngine;
    this.negotiationRepository = negotiationRepository;
    this.resourceLoader = resourceLoader;
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

  @Override
  public String getNotificationTemplate(String templateName) {
    log.info("Getting notification template.");
    validateTemplateName(templateName);
    try {
      org.springframework.core.io.Resource resource =
          resourceLoader.getResource(thymeleafPrefix + templateName + thymeleafSuffix);
      return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      log.error("Failed to get notification template", e);
      throw new EntityNotFoundException(templateName);
    }
  }

  @Override
  public String updateNotificationTemplate(String templateName, String template) {
    log.info("Updating notification template.");
    if (thymeleafPrefix.equals(defaultThymeleafPrefix)) {
      throw new ForbiddenRequestException("Cannot update default templates");
    }
    validateTemplateName(templateName);
    String validateTemplate = validateHtml(template);
    String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;

    org.springframework.core.io.Resource defaultTemplateResource =
        resourceLoader.getResource(targetTemplatePath);

    if (!defaultTemplateResource.exists()) {
      throw new ForbiddenRequestException(
          "Default template for " + targetTemplatePath + " does not exist");
    }

    writeTemplateToFile(targetTemplatePath, validateTemplate);
    return validateTemplate;
  }

  @Override
  public String resetNotificationTemplate(String templateName) {
    log.info("Resetting notification template.");
    if (thymeleafPrefix.equals(defaultThymeleafPrefix)) {
      throw new ForbiddenRequestException("Cannot update default templates");
    }
    validateTemplateName(templateName);
    String defaultTemplatePath = "classpath:/templates/" + templateName + thymeleafSuffix;
    org.springframework.core.io.Resource defaultTemplateResource =
        resourceLoader.getResource(defaultTemplatePath);

    if (!defaultTemplateResource.exists()) {
      throw new EntityNotFoundException(defaultTemplatePath);
    }
    try (InputStream inputStream = defaultTemplateResource.getInputStream()) {
      String defaultTemplate = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

      String targetTemplatePath = thymeleafPrefix + templateName + thymeleafSuffix;
      writeTemplateToFile(targetTemplatePath, defaultTemplate);

      return defaultTemplate;

    } catch (IOException e) {
      log.error("Failed to reset notification template", e);
      throw new RuntimeException("Failed to reset notification template", e);
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
  @Scheduled(cron = "${negotiator.email.frequency-cron-expression:0 0 * * * *}")
  @Async
  public void sendEmailsForNewNotifications() {
    log.debug("Sending new email notifications.");
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

  private void writeTemplateToFile(String targetTemplatePath, String templateContent) {
    try {
      org.springframework.core.io.Resource targetTemplateResource =
          resourceLoader.getResource(targetTemplatePath);

      if (!(targetTemplateResource instanceof WritableResource writable)) {
        throw new IOException("Template is not writable: " + targetTemplatePath);
      }

      try (OutputStream outputStream = writable.getOutputStream()) {
        outputStream.write(templateContent.getBytes(StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      log.error("Failed to write template to file", e);
      throw new RuntimeException("Failed to write template to file", e);
    }
  }

  private String validateHtml(String html) {
    try {
      return Jsoup.parse(html).html();
    } catch (Exception e) {
      throw new UnsupportedMediaTypeException("Template is not valid HTML: " + e.getMessage());
    }
  }

  private void validateTemplateName(String templateName) {
    if (templateName.contains("..") || templateName.contains("/") || templateName.contains("\\")) {
      throw new ForbiddenRequestException("Invalid template name");
    }
  }
}
