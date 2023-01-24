package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.Set;
import javax.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationRequest {

  @Id
  private Long id;
  private String creatorId;
  private String dataSourceId;
  private Set<NegotiableEntity> negotiableEntities;
}
