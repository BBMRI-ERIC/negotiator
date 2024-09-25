package eu.bbmri_eric.negotiator.common;

import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationEvent;
import org.springframework.stereotype.Component;

@Component
@CommonsLog
public class EventListener {
  @org.springframework.context.event.EventListener(ApplicationEvent.class)
  public void logEvent(ApplicationEvent event) {
    log.debug("Event: " + event.getClass().getName() + " published");
  }
}
