package eu.bbmri_eric.negotiator.governance.network;

import java.time.LocalDateTime;
import java.util.List;

public interface NetworkStatsRepository {
  Integer countIgnoredForNetwork(Long networkId);

  Double getMedianResponseForNetwork(Long networkId);

  Integer getNumberOfSuccessfulNegotiationsForNetwork(Long networkId);

  Integer countAllForNetwork(Long networkId);

  List<Object[]> countStatusDistribution(Long networkId);

  Integer getNumberOfNewRequesters(LocalDateTime since, Long networkId);

  Integer getNumberOfActiveRepresentatives(
      LocalDateTime since, LocalDateTime until, Long networkId);
}
