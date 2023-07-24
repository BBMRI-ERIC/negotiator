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

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  private ZonedDateTime recordedAt;
  private NegotiationState changedTo;
}
