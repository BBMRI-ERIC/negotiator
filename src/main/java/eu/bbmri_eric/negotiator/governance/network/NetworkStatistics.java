package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.util.Map;

public interface NetworkStatistics {
  /**
   * Get id of the network
   *
   * @return the id
   */
  Long getNetworkId();

  /**
   * Get the total number of Negotiations associated with this Network.
   *
   * @return a count
   */
  Integer getTotalNumberOfNegotiations();

  /**
   * Get the total number of Negotiations not responded to by a single Organization in the Network.
   *
   * @return a number of ignored Negotiations
   */
  Integer getNumberOfIgnoredNegotiations();

  /**
   * Get a count of Negotiations associated with this network per their state.
   *
   * @return a map of states and their counts
   */
  Map<NegotiationState, Integer> getStatusDistribution();
}
