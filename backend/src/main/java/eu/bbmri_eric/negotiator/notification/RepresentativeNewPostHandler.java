package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.post.NewPostEvent;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

@CommonsLog
@Component
public class RepresentativeNewPostHandler implements EventHandler {


    @Override
    public Class<NewPostEvent> getSupportedEventType() {
        return NewPostEvent.class;
    }

    @Override
    public void notify(ApplicationEvent event) {
            log.info("RepresentativeNewPostHandler.notify()");
    }
}
