package eu.bbmri_eric.negotiator.negotiation.state_machine.resource;

import eu.bbmri_eric.negotiator.common.AuditEntity;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.NegotiationTimelineEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class NegotiationResourceLifecycleRecord extends AuditEntity
    implements NegotiationTimelineEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @ManyToOne
  @JoinColumn(name = "resource_id", referencedColumnName = "id")
  private Resource resource;

  @Enumerated(EnumType.STRING)
  private NegotiationResourceState changedTo;

  @Override
  public String getTriggeredBy() {
    return getCreatedBy().getName();
  }

  @Override
  public String getText() {
    return "%s changed the status of %s to %s"
        .formatted(getTriggeredBy(), resource.getSourceId(), changedTo.getLabel());
  }

  @Override
  public LocalDateTime getTimestamp() {
    return getCreationDate();
  }
}
