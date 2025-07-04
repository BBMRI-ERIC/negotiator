package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.email.EmailService;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.post.Post;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
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
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@CommonsLog
@Transactional
public class OldNotificationServiceImpl implements OldNotificationService {

  NotificationRepository notificationRepository;
  PersonRepository personRepository;
  ModelMapper modelMapper;
  EmailService emailService;
  TemplateEngine templateEngine;
  NegotiationRepository negotiationRepository;

  @Value("${negotiator.frontend-url}")
  private String frontendUrl;

  @Value("${negotiator.emailYoursSincerelyText}")
  private String emailYoursSincerelyText;

  @Value("${negotiator.emailHelpdeskHref}")
  private String emailHelpdeskHref;

  @Value("${negotiator.emailLogo}")
  private String logoURL;

  private static final String EMAIL_TEMPLATE = "email-notification";

  public OldNotificationServiceImpl(
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

  @Override
  public List<NotificationDTO> getNotificationsForUser(Long userId) {
    return notificationRepository.findByRecipientId(userId).stream()
        .map(notification -> modelMapper.map(notification, NotificationDTO.class))
        .collect(Collectors.toList());
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
            .negotiationId(negotiation.getId())
            .recipientId(negotiation.getCreatedBy().getId())
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
                .negotiationId(post.getNegotiation().getId())
                .recipientId(representative.getId())
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
                .negotiationId(post.getNegotiation().getId())
                .recipientId(representative.getId())
                .message(
                    "Negotiation %s had a new post by %s"
                        .formatted(post.getNegotiation().getId(), post.getCreatedBy().getName()))
                .build());
      }
    }
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
            negotiation, representative, "New Negotiation %s ".formatted(negotiation.getId())));
  }

  private Notification buildNewNotification(
      Negotiation negotiation, Person representative, String message) {
    Notification newNotification =
        Notification.builder()
            .negotiationId(negotiation.getId())
            .recipientId(representative.getId())
            .message(message)
            .build();
    return newNotification;
  }

  private void sendEmail(
      @NonNull Person recipient, @NonNull List<NotificationViewDTO> notifications) {
    sendEmail(recipient, notifications, EMAIL_TEMPLATE);
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
        notification.getRecipientId(), notification.getNegotiationId())) {
      return "ROLE_RESEARCHER";
    } else {
      return "ROLE_REPRESENTATIVE";
    }
  }
}
