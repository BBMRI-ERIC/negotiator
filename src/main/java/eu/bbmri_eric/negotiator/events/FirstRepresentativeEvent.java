package eu.bbmri_eric.negotiator.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/** Event for a fist representative being added to a resource. */
@Setter
@Getter
public class FirstRepresentativeEvent extends ApplicationEvent {

  private Long resourceId;

  public FirstRepresentativeEvent(Object source) {
    super(source);
  }

  public FirstRepresentativeEvent(Object source, Long resourceId) {
    super(source);
    this.resourceId = resourceId;
  }
}
