package eu.bbmri_eric.negotiator.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/** Event for a fist representative being added to a resource. */
@Setter
@Getter
public class FirstRepresentativeEvent extends ApplicationEvent {

  private Long resourceId;
  private String sourceId;

  public FirstRepresentativeEvent(Object source, Long resourceId, String sourceId) {
    super(source);
    this.resourceId = resourceId;
    this.sourceId = sourceId;
  }
}
