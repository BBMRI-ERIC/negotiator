package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.Person;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import eu.bbmri_eric.negotiator.user.RepresentativeAddedEvent;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
class RepresentativeAddedHandler implements NotificationStrategy<RepresentativeAddedEvent> {

  public static final String TITLE = "Added as Representative";
  public static final String BODY = "You have been added as a representative for the resource: ";
  private final NotificationService notificationService;
  private final PersonRepository personRepository;
  private final ResourceRepository resourceRepository;

  RepresentativeAddedHandler(
      NotificationService notificationService,
      PersonRepository personRepository,
      ResourceRepository resourceRepository) {
    this.notificationService = notificationService;
    this.personRepository = personRepository;
    this.resourceRepository = resourceRepository;
  }

  @Override
  public Class<RepresentativeAddedEvent> getSupportedEventType() {
    return RepresentativeAddedEvent.class;
  }

  @Override
  @Transactional
  public void notify(RepresentativeAddedEvent event) {
    Long representativeId = event.getRepresentativeId();
    Long resourceId = event.getResourceId();

    Person representative = personRepository.findById(representativeId).orElse(null);
    Resource resource = resourceRepository.findById(resourceId).orElse(null);

    if (representative != null && resource != null) {
      NotificationCreateDTO notification =
          new NotificationCreateDTO(List.of(representativeId), TITLE, BODY + resource.getName());

      notificationService.createNotifications(notification);
      log.info(
          "Notified "
              + representative.getName()
              + " about being assigned as a representative for resource: "
              + resource.getSourceId());
    }
  }
}
