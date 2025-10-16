package eu.bbmri_eric.negotiator.integration.handler;

import eu.bbmri_eric.negotiator.user.AddedRepresentativeEvent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AddedRepresentativeTestEventListener {
  final List<AddedRepresentativeEvent> events = new ArrayList<>();

  @EventListener
  private void onEvent(AddedRepresentativeEvent event) {
    events.add(event);
  }
}
