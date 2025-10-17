package eu.bbmri_eric.negotiator.user;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

/** Event for the assignment of one or more resources to a Person as representative */
@Setter
@Getter
public class AddedRepresentativeEvent extends ApplicationEvent {

  private Long representativeId;
  private String representativeName;
  private Long resourceId;
  private String sourceId;
  private String representativeEmail;

  public AddedRepresentativeEvent(
      Object source,
      Long representativeId,
      String representativeName,
      Long resourceId,
      String sourceId,
      String representativeEmail) {
    super(source);
    this.representativeId = representativeId;
    this.representativeName = representativeName;
    this.resourceId = resourceId;
    this.sourceId = sourceId;
    this.representativeEmail = representativeEmail;
  }
}
