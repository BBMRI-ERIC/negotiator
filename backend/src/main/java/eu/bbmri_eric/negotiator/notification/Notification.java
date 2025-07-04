package eu.bbmri_eric.negotiator.notification;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** A user notification. */
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
@Entity
public class Notification {
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_id_seq")
  @SequenceGenerator(name = "notification_id_seq", initialValue = 10000, allocationSize = 1)
  @Id
  private Long id;

  @JoinColumn(name = "recipient_id")
  @Nonnull
  private Long recipientId;

  @JoinColumn(name = "negotiation_id")
  private String negotiationId;

  @Column(columnDefinition = "TEXT")
  private String title;

  @Column(columnDefinition = "TEXT")
  private String message;

  private LocalDateTime createdAt = LocalDateTime.now();

  private boolean read = false;

  public Notification(
      @Nonnull Long recipientId, String negotiationId, String title, String message) {
    this.recipientId = recipientId;
    this.negotiationId = negotiationId;
    this.title = title;
    this.message = message;
  }

  public Notification(@Nonnull Long recipientId, String title, String message) {
    this.recipientId = recipientId;
    this.title = title;
    this.message = message;
  }
}
