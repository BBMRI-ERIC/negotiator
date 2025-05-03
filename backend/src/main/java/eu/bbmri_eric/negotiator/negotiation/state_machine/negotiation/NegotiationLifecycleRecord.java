package eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation;

import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.negotiation.NegotiationTimelineEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public final class NegotiationLifecycleRecord extends AuditEntity implements NegotiationTimelineEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private NegotiationState changedTo;

  @Override
  public String getTriggeredBy() {
    return getCreatedBy().getName();
  }

  @Override
  public String getText() {
    return "%s changed the status of the Negotiation to  %s".formatted(getTriggeredBy(), changedTo.getLabel());
  }

  @Override
  public LocalDateTime getTimestamp() {
    return getCreationDate();
  }
}
