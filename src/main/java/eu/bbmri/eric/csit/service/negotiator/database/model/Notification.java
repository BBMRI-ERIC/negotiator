package eu.bbmri.eric.csit.service.negotiator.database.model;

import jakarta.annotation.Nonnull;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;

@ToString
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class Notification {
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notification_id_seq")
  @SequenceGenerator(name = "notification_id_seq", initialValue = 10000, allocationSize = 1)
  @Id
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "person_id")
  @Nonnull
  private Person recipient;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "negotiation_id")
  @Nonnull
  private Negotiation negotiation;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  private NotificationEmailStatus emailStatus;

  @CreatedDate private LocalDateTime creationDate;
}
