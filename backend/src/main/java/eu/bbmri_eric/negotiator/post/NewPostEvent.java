package eu.bbmri_eric.negotiator.post;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class NewPostEvent extends ApplicationEvent {
  private String postId;
  private String negotiationId;
  private Long userId;
  private Long organizationId;

  public NewPostEvent(Object source) {
    super(source);
  }

  public NewPostEvent(
      Object source, String postId, String negotiationId, Long userId, Long organizationId) {
    super(source);
    this.postId = postId;
    this.negotiationId = negotiationId;
    this.userId = userId;
    this.organizationId = organizationId;
  }

  public NewPostEvent(Object source, String postId, String negotiationId, Long userId) {
    super(source);
    this.postId = postId;
    this.negotiationId = negotiationId;
    this.userId = userId;
  }
}
