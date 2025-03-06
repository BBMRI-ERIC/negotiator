package eu.bbmri_eric.negotiator.governance.network.stats;

import static org.apache.commons.math3.util.Precision.round;

import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NetworkStatisticsServiceImpl implements NetworkStatisticsService {

  private final NegotiationRepository negotiationRepository;
  private final NetworkRepository networkRepository;

  public NetworkStatisticsServiceImpl(
      NegotiationRepository negotiationRepository, NetworkRepository networkRepository) {
    this.negotiationRepository = negotiationRepository;
    this.networkRepository = networkRepository;
  }

  @Override
  public NetworkStatistics getBasicNetworkStats(Long networkId, NetworkStatsFilter filter) {
    Integer count =
        negotiationRepository.countAllForNetwork(filter.getSince(), filter.getUntil(), networkId);
    Double median =
        negotiationRepository.getMedianResponseForNetwork(
            filter.getSince(), filter.getUntil(), networkId);
    try {
      median = round(median, 2);
    } catch (NullPointerException e) {
      median = null;
    }
    List<String> ignoredIds =
        networkRepository.getIgnoredForNetwork(filter.getSince(), filter.getUntil(), networkId);
    List<String> successfulIds =
        networkRepository.getSuccessfulForNetwork(filter.getSince(), filter.getUntil(), networkId);
    Map<NegotiationState, Integer> states =
        networkRepository
            .countStatusDistribution(filter.getSince(), filter.getUntil(), networkId)
            .stream()
            .collect(
                Collectors.toMap(
                    result -> (NegotiationState) result[0],
                    result -> ((Long) result[1]).intValue()));
    Map<String, List<String>> ids = new HashMap<>();
    ids.put("Ignored", ignoredIds);
    ids.put("Successful", successfulIds);
    return SimpleNetworkStatistics.builder()
        .networkId(networkId)
        .numberOfNewRequesters(
            networkRepository.getNumberOfNewRequesters(
                filter.getSince(), filter.getUntil(), networkId))
        .medianResponseTime(median)
        .numberOfIgnoredNegotiations(ignoredIds.size())
        .numberOfSuccessfulNegotiations(successfulIds.size())
        .numberOfActiveRepresentatives(
            networkRepository.getNumberOfActiveRepresentatives(
                filter.getSince(), filter.getUntil(), networkId))
        .totalNumberOfNegotiations(count)
        .statusDistribution(states)
        .negotiationIds(ids)
        .build();
  }
}
