package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.time.ZonedDateTime;
import javax.persistence.*;
import lombok.*;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public final class NegotiationLifecycleRecord {

  @Id @GeneratedValue private Long id;
  private ZonedDateTime recordedAt;

  @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
  @JoinColumn(name = "person_id")
  private Person changedBy;

  private NegotiationState changedTo;
}
