package eu.bbmri_eric.negotiator.notification.internal;

import eu.bbmri_eric.negotiator.notification.NotificationCreateDTO;
import eu.bbmri_eric.negotiator.notification.NotificationService;
import eu.bbmri_eric.negotiator.user.AddedRepresentativeEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Listens for requests to send an email notification to a user, when he is added to one or more
 * resources. The handler buffers one or more new representative added events and collects them into
 * a unique notification, sent to the Notification Service on a fixed time rate.
 */
@Component
@CommonsLog
public class AddedRepresentativeHandler implements ApplicationListener<AddedRepresentativeEvent> {

  private final String TITLE = "You have been added as a representative for one or more resources";
  private final String TEXT =
      """
      You have been added as a representative for one or more resources.


      Please log in to the BBMRI Negotiator to review all the ongoing negotiations involving these resources.""";

  private final Map<Long, List<AddedRepresentativeEvent>> eventsBuffer = new ConcurrentHashMap<>();
  private final NotificationService notificationService;

  public AddedRepresentativeHandler(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @Scheduled(fixedRate = 300000)
  public void flushEventBuffer() {
    if (eventsBuffer.isEmpty()) return;

    Map<Long, List<AddedRepresentativeEvent>> toHandle = new HashMap<>(eventsBuffer);
    eventsBuffer.clear();

    handleEvents(toHandle);
  }

  @Override
  public void onApplicationEvent(AddedRepresentativeEvent event) {
    eventsBuffer
        .computeIfAbsent(event.getRepresentativeId(), k -> new CopyOnWriteArrayList<>())
        .add(event);
  }

  public void handleEvents(Map<Long, List<AddedRepresentativeEvent>> events) {

    events.forEach(
        (representativeId, addedRepresentativeEvents) -> {
          NotificationCreateDTO notification =
              new NotificationCreateDTO(List.of(representativeId), TITLE, TEXT, null);
          notificationService.createNotifications(notification);
          log.info(
              "Sent notification to representative:"
                  + representativeId
                  + "to notify about"
                  + addedRepresentativeEvents.size()
                  + " added resources.");
        });
  }
}
