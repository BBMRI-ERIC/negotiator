package eu.bbmri_eric.negotiator.integration.handler;

import eu.bbmri_eric.negotiator.events.FirstRepresentativeEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class TestEventListener {
  final List<FirstRepresentativeEvent> events = new ArrayList<>();

  @EventListener
  void onEvent(FirstRepresentativeEvent event) {
    events.add(event);
  }

  void reset() {
    events.clear();
  }
}
