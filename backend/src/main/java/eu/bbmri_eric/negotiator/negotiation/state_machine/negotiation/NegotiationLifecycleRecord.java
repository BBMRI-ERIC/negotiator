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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public final class NegotiationLifecycleRecord extends AuditEntity
    implements NegotiationTimelineEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Enumerated(EnumType.STRING)
  private NegotiationState changedTo;

  /**
   * A record of the Negotiation reaching the State with this name.
   *
   * <p>{@link eu.bbmri_eric.negotiator.negotiation.Negotiation} carries its State as a name, but
   * this row stores an enum, so the name is converted here, next to the enums. A name that matches
   * no State fails here instead of reaching the history table. Null stays null.
   *
   * @param stateName the name of the State reached, or null
   */
  public static NegotiationLifecycleRecord forStateNamed(String stateName) {
    return NegotiationLifecycleRecord.builder()
        .changedTo(stateName == null ? null : NegotiationState.valueOf(stateName))
        .build();
  }

  @Override
  public String getTriggeredBy() {
    return getCreatedBy().getName();
  }

  @Override
  public String getText() {
    return "%s changed the status of the Negotiation to %s"
        .formatted(getTriggeredBy(), changedTo.getLabel());
  }

  @Override
  public LocalDateTime getTimestamp() {
    return getCreationDate();
  }
}
