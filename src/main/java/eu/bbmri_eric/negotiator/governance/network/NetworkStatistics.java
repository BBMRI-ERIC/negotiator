package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Interface defining statistical information about a network.")
public interface NetworkStatistics {

  /**
   * Get id of the network.
   *
   * @return the id
   */
  @Schema(description = "Unique identifier of the network", example = "101")
  Long getNetworkId();

  /**
   * Get the total number of Negotiations associated with this Network.
   *
   * @return a count
   */
  @Schema(description = "Total number of negotiations in the network", example = "150")
  Integer getTotalNumberOfNegotiations();

  /**
   * Get the total number of Negotiations not responded to by a single Organization in the Network.
   *
   * @return a number of ignored Negotiations
   */
  @Schema(description = "Number of negotiations ignored in the network", example = "10")
  Integer getNumberOfIgnoredNegotiations();

  /**
   * Get the median response time for resources in this Network.
   *
   * @return a median amount of days it takes resources in this Network to respond. E.g., 10.6
   */
  @Schema(description = "Median response time for negotiations in days", example = "10.6")
  Double getMedianResponseTime();

  /**
   * Get the number of successful Negotiations. At least one resource of this network was made
   * accessible.
   *
   * @return a number of successful Negotiations
   */
  @Schema(description = "Number of successful negotiations in the network", example = "120")
  Integer getNumberOfSuccessfulNegotiations();

  /**
   * Get the number of new Requesters that have not submitted a request prior to the period for
   * which this data has been generated.
   *
   * @return a number of new Users
   */
  @Schema(description = "Number of new requesters in the network", example = "30")
  Integer getNumberOfNewRequesters();

  /**
   * Get the number of Representatives from this network that have posted at least one comment or
   * updated the status of a Resource.
   *
   * @return a number of active Users
   */
  @Schema(description = "Number of active representatives in the network", example = "25")
  Integer getNumberOfActiveRepresentatives();

  /**
   * Get a count of Negotiations associated with this network per their state.
   *
   * @return a map of states and their counts
   */
  @Schema(
      description = "Distribution of negotiation statuses in the network",
      example = "{\"OPEN\": 50, \"CLOSED\": 90, \"PENDING\": 10}")
  Map<NegotiationState, Integer> getStatusDistribution();
}

