package eu.bbmri_eric.negotiator.governance.network;

import static org.apache.commons.math3.util.Precision.round;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NetworkStatisticsServiceImpl implements NetworkStatisticsService {

  private final NegotiationRepository negotiationRepository;

  public NetworkStatisticsServiceImpl(NegotiationRepository negotiationRepository) {
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public NetworkStatistics getBasicNetworkStats(Long networkId) {
    Integer count = negotiationRepository.countAllForNetwork(networkId);
    Double median = negotiationRepository.getMedianResponseForNetwork(networkId);
    try {
      median = round(median, 2);
    } catch (NullPointerException e) {
      median = null;
    }
    Map<NegotiationState, Integer> states =
        negotiationRepository.countStatusDistribution(networkId).stream()
            .collect(
                Collectors.toMap(
                    result -> (NegotiationState) result[0],
                    result -> ((Long) result[1]).intValue()));
    return SimpleNetworkStatistics.builder()
        .networkId(networkId)
        .numberOfNewRequesters(
            negotiationRepository.getNumberOfNewRequesters(
                LocalDateTime.now().minusYears(50), networkId))
        .medianResponseTime(median)
        .numberOfIgnoredNegotiations(negotiationRepository.countIgnoredForNetwork(networkId))
        .numberOfSuccessfulNegotiations(
            negotiationRepository.getNumberOfSuccessfulNegotiationsForNetwork(networkId))
        .numberOfActiveRepresentatives(
            negotiationRepository.getNumberOfActiveRepresentatives(
                LocalDateTime.now().minusYears(50), LocalDateTime.now(), networkId))
        .totalNumberOfNegotiations(count)
        .statusDistribution(states)
        .build();
  }
}
