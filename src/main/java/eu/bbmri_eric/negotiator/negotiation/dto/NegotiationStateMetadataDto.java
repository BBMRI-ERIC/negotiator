package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.server.core.Relation;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.NegotiationStateMetadata} */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Relation(collectionRelation = "states", itemRelation = "state")
public class NegotiationStateMetadataDto implements Serializable {
  private NegotiationState value;
  private String label;
  private String description;
}
