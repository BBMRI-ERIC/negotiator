package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class NegotiationResources {

  private String negotiationId;
  private List<Resource> resources;
}
