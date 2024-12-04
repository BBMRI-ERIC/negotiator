package eu.bbmri_eric.negotiator.negotiation.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.server.core.Relation;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Relation(collectionRelation = "events", itemRelation = "event")
public class NegotiationEventMetadataDTO implements Serializable {
  private String value;
  private String label;
  private String description;
}
