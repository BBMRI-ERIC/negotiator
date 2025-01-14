package eu.bbmri_eric.negotiator.governance.network.stats;

public interface NetworkStatisticsService {
  /**
   * Get basic network statistics.
   *
   * @return a DTO with the stats.
   */
  NetworkStatistics getBasicNetworkStats(Long networkId, NetworkStatsFilter filter);
}
