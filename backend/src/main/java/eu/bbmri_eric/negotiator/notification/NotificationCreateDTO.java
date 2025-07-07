package eu.bbmri_eric.negotiator.notification;

import jakarta.annotation.Nonnull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

// TODO: Add swagger annotations
@Getter
@Setter
public class NotificationCreateDTO {
  @Nonnull private List<Long> userIds;
  @Nonnull private String title;
  @Nonnull private String body;
  private String negotiationId;

  public NotificationCreateDTO(
      @Nonnull List<Long> userIds, @Nonnull String title, @Nonnull String body) {
    this.userIds = userIds;
    this.title = title;
    this.body = body;
  }

  public NotificationCreateDTO(
      @Nonnull List<Long> userIds,
      @Nonnull String title,
      @Nonnull String body,
      String negotiationId) {
    this.userIds = userIds;
    this.title = title;
    this.body = body;
    this.negotiationId = negotiationId;
  }
}
