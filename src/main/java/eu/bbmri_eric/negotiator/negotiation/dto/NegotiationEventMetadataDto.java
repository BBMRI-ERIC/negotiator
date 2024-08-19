package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationEvent;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.server.core.Relation;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.NegotiationEventMetadata} */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Relation(collectionRelation = "events", itemRelation = "event")
public class NegotiationEventMetadataDto implements Serializable {
  private NegotiationEvent value;
  private String label;
  private String description;
}
