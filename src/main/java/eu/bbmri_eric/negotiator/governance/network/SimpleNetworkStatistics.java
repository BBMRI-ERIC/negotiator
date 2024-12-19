package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "DTO for providing basic statistics about a network.")
public class SimpleNetworkStatistics implements NetworkStatistics {
  @Schema(description = "Unique identifier of the network", example = "101")
  private Long networkId;

  @Schema(description = "Total number of negotiations in the network", example = "150")
  private Integer totalNumberOfNegotiations;

  @Schema(description = "Number of negotiations ignored in the network", example = "10")
  private Integer numberOfIgnoredNegotiations;

  @Schema(description = "Median response time for negotiations in days", example = "12.5")
  private Double medianResponseTime;

  @Schema(description = "Number of successful negotiations in the network", example = "120")
  private Integer numberOfSuccessfulNegotiations;

  @Schema(description = "Number of new requesters in the network", example = "30")
  private Integer numberOfNewRequesters;

  @Schema(description = "Number of active representatives in the network", example = "25")
  private Integer numberOfActiveRepresentatives;

  @Schema(
      description = "Distribution of negotiation statuses in the network",
      example = "{\"IN_PROGRESS\": 50, \"SUBMITTED\": 90, \"ABANDONED\": 10}")
  private Map<NegotiationState, Integer> statusDistribution = new HashMap<>();
}
