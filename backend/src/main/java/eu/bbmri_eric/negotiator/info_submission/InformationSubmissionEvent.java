package eu.bbmri_eric.negotiator.info_submission;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class InformationSubmissionEvent extends ApplicationEvent {
  private final String negotiationId;

  public InformationSubmissionEvent(Object source, String negotiationId) {
    super(source);
    this.negotiationId = negotiationId;
  }
}
