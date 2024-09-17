package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SimpleNetworkStatistics implements NetworkStatistics {
  private Long networkId;
  private Integer totalNumberOfNegotiations;
  private Map<NegotiationState, Integer> statusDistribution = new HashMap<>();
}
