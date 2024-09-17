package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class NetworkStatisticsServiceImpl implements NetworkStatisticsService {

  private final NetworkRepository networkRepository;
  private final NegotiationRepository negotiationRepository;

  public NetworkStatisticsServiceImpl(
      NetworkRepository networkRepository, NegotiationRepository negotiationRepository) {
    this.networkRepository = networkRepository;
    this.negotiationRepository = negotiationRepository;
  }

  @Override
  public NetworkStatistics getBasicNetworkStats(Long networkId) {
    Integer count = negotiationRepository.countAllForNetwork(networkId);
    Map<NegotiationState, Integer> states =
        negotiationRepository.countStatusDistribution(networkId).stream()
            .collect(
                Collectors.toMap(
                    result -> (NegotiationState) result[0],
                    result -> ((Long) result[1]).intValue()));
    return new SimpleNetworkStatistics(networkId, count, states);
  }
}
