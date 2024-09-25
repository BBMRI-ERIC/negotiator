package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NegotiationResourceLinkId implements Serializable {
  @ManyToOne(optional = false)
  @JoinColumn
  private Negotiation negotiation;

  @JoinColumn
  @ManyToOne(optional = false) private Resource resource;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    NegotiationResourceLinkId that = (NegotiationResourceLinkId) o;
    return Objects.equals(negotiation, that.negotiation) && Objects.equals(resource, that.resource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(negotiation, resource);
  }
}
