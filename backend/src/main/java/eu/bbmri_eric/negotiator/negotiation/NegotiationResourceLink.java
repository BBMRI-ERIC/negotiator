package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;
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
public class NegotiationResourceLink {
  @EmbeddedId private NegotiationResourceLinkId id;

  @Enumerated(EnumType.STRING)
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NegotiationResourceLink link = (NegotiationResourceLink) o;
    return Objects.equals(id, link.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }
}
