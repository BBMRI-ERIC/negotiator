package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import java.util.Set;

public class CustomNegotiationBuilder extends Negotiation.NegotiationBuilder {
  private Set<Resource> resources;

  @Override
  public CustomNegotiationBuilder resources(Set<Resource> resources) {
    this.resources = resources;
    return this;
  }

  @Override
  public Negotiation build() {
    Negotiation negotiation = super.build();
    if (this.resources != null) {
      negotiation.setResources(this.resources);
    }
    return negotiation;
  }
}
