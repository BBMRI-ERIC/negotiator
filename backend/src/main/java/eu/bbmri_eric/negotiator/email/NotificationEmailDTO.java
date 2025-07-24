package eu.bbmri_eric.negotiator.email;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Relation(itemRelation = "notificationEmail", collectionRelation = "notificationEmails")
@Schema(description = "Data Transfer Object for NotificationEmail entity")
public class NotificationEmailDTO {

  @Schema(description = "Unique identifier of the notification email", example = "12345")
  private Long id;

  @Schema(description = "Email address recipient", example = "user@example.com")
  private String address;

  @Schema(description = "Email message content", example = "Your negotiation has been approved...")
  private String message;

  @Schema(description = "Date and time when the email was sent", example = "2024-07-11T10:30:00")
  private LocalDateTime sentAt;

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    NotificationEmailDTO that = (NotificationEmailDTO) o;
    return Objects.equals(id, that.id)
        && Objects.equals(address, that.address)
        && Objects.equals(message, that.message)
        && Objects.equals(sentAt, that.sentAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, address, message, sentAt);
  }
}
