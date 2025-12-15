package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.ResourceStateChangeEvent;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.settings.AdminSettingsRepository;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ResourceStateChangeHandler implements NotificationStrategy<ResourceStateChangeEvent> {

  public static final String TITLE = "Request Status update";
  public static final String BODY =
      "Resource %s had a change of status in your request %s, from %s to %s";
  private final NegotiationRepository negotiationRepository;
  private final NotificationService notificationService;
  private final PersonRepository personRepository;
  private final AdminSettingsRepository adminSettingsRepository;

  public ResourceStateChangeHandler(
      NegotiationRepository negotiationRepository,
      NotificationService notificationService,
      PersonRepository personRepository,
      AdminSettingsRepository adminSettingsRepository) {
    this.negotiationRepository = negotiationRepository;
    this.notificationService = notificationService;
    this.personRepository = personRepository;
    this.adminSettingsRepository = adminSettingsRepository;
  }

  @Override
  public Class<ResourceStateChangeEvent> getSupportedEventType() {
    return ResourceStateChangeEvent.class;
  }

  @Override
  @Transactional
  public void notify(ResourceStateChangeEvent event) {
    Negotiation negotiation =
        negotiationRepository
            .findById(event.getNegotiationId())
            .orElseThrow(() -> new EntityNotFoundException(event.getNegotiationId()));
    notificationService.createNotifications(
        new NotificationCreateDTO(
            List.of(negotiation.getCreatedBy().getId()),
            TITLE,
            BODY.formatted(
                event.getResourceId(),
                negotiation.getTitle(),
                event.getFromState().getLabel(),
                event.getToState().getLabel()),
            event.getNegotiationId()));
    if (adminSettingsRepository.getSendNegotiationUpdatesNotifications()) {
      List<Person> administrators = personRepository.findAllByAdminIsTrue();
      String title = "Admin notification update for negotiation: " + negotiation.getId();
      String body =
          "There are updates on negotiation "
              + negotiation.getId()
              + ".\n"
              + "Please log into the Negotiator for more details.";
      notificationService.createNotifications(
          new NotificationCreateDTO(
              administrators.stream().map(Person::getId).toList(),
              title,
              body,
              event.getNegotiationId()));
    }
  }
}
