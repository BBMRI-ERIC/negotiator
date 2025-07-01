package eu.bbmri_eric.negotiator.notification;

import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.notification.email.NotificationEmailStatus;
import eu.bbmri_eric.negotiator.user.Person;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Builder
@Entity
public class Notification extends AuditEntity {
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_id_seq")
  @SequenceGenerator(name = "notification_id_seq", initialValue = 10000, allocationSize = 1)
  @Id
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "recipient_id")
  @Nonnull
  @ToString.Exclude
  private Person recipient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "negotiation_id")
  @ToString.Exclude
  private Negotiation negotiation;

  @Column(columnDefinition = "TEXT")
  private String title;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  private NotificationEmailStatus emailStatus;

  public Notification(
      @Nonnull Person recipient,
      @Nonnull Negotiation negotiation,
      String message,
      NotificationEmailStatus emailStatus) {
    this.recipient = recipient;
    this.negotiation = negotiation;
    this.message = message;
    this.emailStatus = emailStatus;
  }

  public Notification(
      @Nonnull Person recipient,
      @Nonnull Negotiation negotiation,
      String title,
      String message,
      NotificationEmailStatus emailStatus) {
    this.recipient = recipient;
    this.negotiation = negotiation;
    this.title = title;
    this.message = message;
    this.emailStatus = emailStatus;
  }

  public Notification(@Nonnull Person recipient, String title, String message) {
    this.recipient = recipient;
    this.title = title;
    this.message = message;
  }
}
