package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.time.ZonedDateTime;
import java.util.Objects;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NegotiationLifecycleRecord that = (NegotiationLifecycleRecord) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
