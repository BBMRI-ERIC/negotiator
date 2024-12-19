package eu.bbmri_eric.negotiator.governance.network;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NetworkStatsRepositoryImpl implements NetworkStatsRepository {
  EntityManager entityManager;

  public NetworkStatsRepositoryImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Integer countIgnoredForNetwork(Long networkId) {
    return 0;
  }

  @Override
  public Double getMedianResponseForNetwork(Long networkId) {
    return 0.0;
  }

  @Override
  public Integer getNumberOfSuccessfulNegotiationsForNetwork(Long networkId) {
    return 0;
  }

  @Override
  public Integer countAllForNetwork(Long networkId) {
    return 0;
  }

  @Override
  public List<Object[]> countStatusDistribution(Long networkId) {
    return List.of();
  }

  @Override
  public Integer getNumberOfNewRequesters(LocalDateTime since, Long networkId) {
    return 0;
  }

  @Override
  public Integer getNumberOfActiveRepresentatives(
      LocalDateTime since, LocalDateTime until, Long networkId) {
    return 0;
  }
}
