package eu.bbmri_eric.negotiator.governance.resource.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.server.core.Relation;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Relation(collectionRelation = "states", itemRelation = "state")
public class ResourceStateMetadataDto implements Serializable {
  private NegotiationResourceState value;
  private String label;
  private String description;
}
