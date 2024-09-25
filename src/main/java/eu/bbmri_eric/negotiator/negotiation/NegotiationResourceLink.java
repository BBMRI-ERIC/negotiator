package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "negotiation_resource_link")
public class NegotiationResourceLink {
  @EmbeddedId private NegotiationResourceLinkId id;

  private NegotiationResourceState currentState;

  public NegotiationResourceLink(
      Negotiation negotiation, Resource resource, NegotiationResourceState currentState) {
    this.id = new NegotiationResourceLinkId(negotiation, resource);
    this.currentState = currentState;
  }

  public Negotiation getNegotiation() {
    return this.id.getNegotiation();
  }

  public Resource getResource() {
    return this.id.getResource();
  }
}
