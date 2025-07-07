package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.post.NewPostEvent;
import eu.bbmri_eric.negotiator.user.PersonService;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
class NewPostHandler implements NotificationStrategy<NewPostEvent> {
    private final NotificationService notificationService;
    private final PersonService personService;

    NewPostHandler(NotificationService notificationService, PersonService personService) {
        this.notificationService = notificationService;
        this.personService = personService;
    }


    @Override
    public Class<NewPostEvent> getSupportedEventType() {
        return NewPostEvent.class;
    }

    @Override
    public void notify(NewPostEvent event) {
        if (event.getOrganizationId() != null) {
            personService.findAllByOrganizationId(event.getOrganizationId())
        }
    }
}
