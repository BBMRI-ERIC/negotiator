package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A user notification.
 */
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

  public Notification(@Nonnull Long recipientId, String negotiationId, String title, String message) {
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
