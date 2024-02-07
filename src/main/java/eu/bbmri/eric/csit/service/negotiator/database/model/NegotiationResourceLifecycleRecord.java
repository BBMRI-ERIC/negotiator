package eu.bbmri.eric.csit.service.negotiator.database.model;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
public class NegotiationResourceLifecycleRecord extends AuditEntity {

  @ManyToOne
  @JoinColumn(name = "resource_id", referencedColumnName = "id")
  private Resource resource;

  private LocalDateTime recordedAt;

  @Enumerated(EnumType.STRING)
  private NegotiationResourceState changedTo;
}
