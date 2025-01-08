package eu.bbmri_eric.negotiator.governance.network.stats;

import java.time.LocalDate;
import java.util.List;

public interface NetworkStatsRepository {
  Integer countIgnoredForNetwork(LocalDate since, LocalDate until, Long networkId);

  Double getMedianResponseForNetwork(LocalDate since, LocalDate until, Long networkId);

  Integer getNumberOfSuccessfulNegotiationsForNetwork(
      LocalDate since, LocalDate until, Long networkId);

  Integer countAllForNetwork(LocalDate since, LocalDate until, Long networkId);

  List<Object[]> countStatusDistribution(LocalDate since, LocalDate until, Long networkId);

  Integer getNumberOfNewRequesters(LocalDate since, LocalDate until, Long networkId);

  Integer getNumberOfActiveRepresentatives(LocalDate since, LocalDate until, Long networkId);
}
