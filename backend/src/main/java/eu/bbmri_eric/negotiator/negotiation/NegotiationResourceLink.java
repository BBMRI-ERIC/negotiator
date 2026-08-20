package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import jakarta.persistence.Column;
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

  /**
   * The Definition Version this Resource's Lifecycle is pinned to. Per link rather than per
   * Negotiation, because two Resources of one Negotiation may resolve to different Definition
   * Families. Set once, when the Resource's Lifecycle starts, and never afterwards.
   *
   * <p>Null on every row that predates the pin, until the data cutover backfills them.
   *
   * <p>A plain id rather than an association, deliberately. This entity is on read paths that exist
   * today, and an association would let one of them traverse into the Lifecycle Definition graph.
   */
  @Setter(AccessLevel.NONE)
  @Column(updatable = false)
  private Long lifecycleDefinitionId;

  public NegotiationResourceLink(
      Negotiation negotiation, Resource resource, NegotiationResourceState currentState) {
    this(negotiation, resource, currentState, null);
  }

  /** A link whose Resource Lifecycle has already resolved its Definition Version. */
  public NegotiationResourceLink(
      Negotiation negotiation,
      Resource resource,
      NegotiationResourceState currentState,
      Long lifecycleDefinitionId) {
    this.id = new NegotiationResourceLinkId(negotiation, resource);
    this.currentState = currentState;
    this.lifecycleDefinitionId = lifecycleDefinitionId;
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
