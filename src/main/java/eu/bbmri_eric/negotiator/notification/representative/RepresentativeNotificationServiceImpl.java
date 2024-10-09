package eu.bbmri_eric.negotiator.notification.representative;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.notification.NotificationRepository;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class RepresentativeNotificationServiceImpl implements RepresentativeNotificationService {
  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;
  private final NotificationRepository notificationRepository;
  private final ApplicationEventPublisher eventPublisher;

  public RepresentativeNotificationServiceImpl(
      NegotiationRepository negotiationRepository,
      PersonRepository personRepository,
      NotificationRepository notificationRepository,
      ApplicationEventPublisher eventPublisher) {
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
    this.notificationRepository = notificationRepository;
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void notifyAboutPendingNegotiations() {}
}
