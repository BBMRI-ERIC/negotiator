package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Set;

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
