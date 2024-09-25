package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import jakarta.persistence.Embeddable;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NegotiationResourceLinkId {
  @ManyToOne private Negotiation negotiation;

  @ManyToOne private Resource resource;
}
