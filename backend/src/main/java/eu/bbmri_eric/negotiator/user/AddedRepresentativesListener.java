package eu.bbmri_eric.negotiator.user;

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
 * resources. The listener buffers one or more new representative added events and collects them
 * into a unique email, sent after a certain amount of time.
 */
@Component
@CommonsLog
public class AddedRepresentativesListener implements ApplicationListener<AddedRepresentativeEvent> {

  private final AddedRepresentativesHandler handler;
  private final Map<Long, List<AddedRepresentativeEvent>> eventsBuffer = new ConcurrentHashMap<>();

  public AddedRepresentativesListener(AddedRepresentativesHandler handler) {
    this.handler = handler;
  }

  @Scheduled(fixedRate = 120000)
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
    handler.notifyAddedRepresentatives((events));
  }
}
