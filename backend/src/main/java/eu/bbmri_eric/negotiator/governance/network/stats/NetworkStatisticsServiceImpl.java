package eu.bbmri_eric.negotiator.governance.network.stats;

import static org.apache.commons.math3.util.Precision.round;

import eu.bbmri_eric.negotiator.governance.network.NetworkRepository;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
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
        negotiationRepository.countAllNotDraftForNetwork(
            filter.getSince(), filter.getUntil(), networkId);
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
    Map<String, Integer> states =
        networkRepository
            .countStatusDistribution(filter.getSince(), filter.getUntil(), networkId)
            .stream()
            .collect(
                Collectors.toMap(
                    result -> stateNameOf(result[0]), result -> ((Long) result[1]).intValue()));
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

  /**
   * Reads the State name from the untyped projection that {@code countStatusDistribution} returns.
   * The value is an enum while {@code Negotiation.currentState} is an {@code @Enumerated} field and
   * a String otherwise, so this reads both. The projection is an {@code Object}, so a cast would
   * compile either way and fail only at runtime.
   */
  private static String stateNameOf(Object projectedState) {
    return projectedState instanceof Enum<?> state ? state.name() : (String) projectedState;
  }
}
