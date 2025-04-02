package eu.bbmri_eric.negotiator.webhook;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entity representing a delivery attempt for a webhook. Each delivery is associated with a {@link
 * Webhook} and contains a JSON payload, a success flag, and a timestamp (stored in the "at" column)
 * of when it was created.
 */
@Getter
@Setter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
public class Delivery {

  @Id private String id;

  @Column(nullable = false)
  private Long webhookId;

  @JdbcTypeCode(SqlTypes.JSON)
  private String content;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private LocalDateTime at;

  /** Optional HTTP status code from the delivery attempt. */
  private Integer httpStatusCode;

  /** Optional error message if the delivery attempt failed. */
  private String errorMessage;

  public Delivery(String content, Integer httpStatusCode, String errorMessage) {
    validateStatusAndMessage(httpStatusCode, errorMessage);
    this.content = content;
    this.httpStatusCode = httpStatusCode;
    this.errorMessage = errorMessage;
  }

  public Delivery(String content, Integer httpStatusCode) {
    validateStatusAndMessage(httpStatusCode, errorMessage);
    this.content = content;
    this.httpStatusCode = httpStatusCode;
  }

  private static void validateStatusAndMessage(Integer httpStatusCode, String errorMessage) {
    if (httpStatusCode != null
        && (httpStatusCode < 200 || httpStatusCode > 299)
        && (errorMessage == null || errorMessage.isEmpty())) {
      throw new IllegalArgumentException("Non-200 HTTP status code requires an error message.");
    }
  }

  @PrePersist
  private void generateId() {
    if (id == null || id.isEmpty()) {
      id = UUID.randomUUID().toString();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    Delivery delivery = (Delivery) o;
    return Objects.equals(id, delivery.id)
        && Objects.equals(webhookId, delivery.webhookId)
        && Objects.equals(content, delivery.content)
        && Objects.equals(at, delivery.at)
        && Objects.equals(httpStatusCode, delivery.httpStatusCode)
        && Objects.equals(errorMessage, delivery.errorMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, webhookId, content, at, httpStatusCode, errorMessage);
  }
}
