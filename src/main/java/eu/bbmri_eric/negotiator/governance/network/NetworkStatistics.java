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
   * Get the median response time for resources in this Network.
   *
   * @return a median amount of days it takes resources in this Network to respond. E.g., 10.6
   */
  Double getMedianResponseTime();

  /**
   * Get the number of successful Negotiations. At least one resource of this network was made
   * accessible.
   *
   * @return a number of successful Negotiations
   */
  Integer getNumberOfSuccessfulNegotiations();

  /**
   * Get the number of new Requesters that have not submitted a request prior to the period for
   * which this data has been generated.
   *
   * @return a number of new Users
   */
  Integer getNumberOfNewRequesters();

  /**
   * Get the number of Representatives from this network that have posted at least one comment or
   * updated the status of a Resource.
   *
   * @return a number of active Users
   */
  Integer getNumberOfActiveRepresentatives();

  /**
   * Get a count of Negotiations associated with this network per their state.
   *
   * @return a map of states and their counts
   */
  Map<NegotiationState, Integer> getStatusDistribution();
}
