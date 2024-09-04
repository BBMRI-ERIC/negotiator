package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
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
    return new SimpleNetworkStatistics(networkId, count, null);
  }
}
