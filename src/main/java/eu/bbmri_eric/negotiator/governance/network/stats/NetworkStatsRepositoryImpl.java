package eu.bbmri_eric.negotiator.governance.network.stats;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class NetworkStatsRepositoryImpl implements NetworkStatsRepository {
  @PersistenceContext EntityManager entityManager;

  public NetworkStatsRepositoryImpl(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Integer countIgnoredForNetwork(LocalDate since, LocalDate until, Long networkId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    """
        SELECT COUNT(DISTINCT n.id)
        FROM negotiation n
        LEFT JOIN negotiation_resource_link nrl ON n.id = nrl.negotiation_id
        LEFT JOIN network_resources_link l ON nrl.resource_id = l.resource_id
        WHERE l.network_id = :networkId
          AND (nrl.current_state = 'REPRESENTATIVE_CONTACTED' OR nrl.current_state = 'REPRESENTATIVE_UNREACHABLE')
          AND DATE(n.creation_date) > :since
          AND DATE(n.creation_date) <= :until
    """)
                .setParameter("networkId", networkId)
                .setParameter("since", since)
                .setParameter("until", until)
                .getSingleResult())
        .intValue();
  }

  @Override
  public Double getMedianResponseForNetwork(LocalDate since, LocalDate until, Long networkId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    """
            SELECT PERCENTILE_CONT(0.5)
           WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (nrlr.creation_date - n.creation_date)) / 86400)
           AS median_days
            FROM Negotiation n
            LEFT JOIN public.negotiation_resource_lifecycle_record nrlr on n.id = nrlr.negotiation_id
            left join public.network_resources_link nrl on nrlr.resource_id = nrl.resource_id
            where nrl.network_id = :networkId and (nrlr.changed_to = 'CHECKING_AVAILABILITY' or nrlr.changed_to = 'RESOURCE_UNAVAILABLE')
                    and Date(n.creation_date) > :since and Date(n.creation_date) <= :until
        """)
                .setParameter("networkId", networkId)
                .setParameter("since", since)
                .setParameter("until", until)
                .getSingleResult())
        .doubleValue();
  }

  @Override
  public Integer getNumberOfSuccessfulNegotiationsForNetwork(
      LocalDate since, LocalDate until, Long networkId) {
    return ((Number)
            entityManager
                .createQuery(
                    """
            SELECT count ( distinct n.id)
            FROM Negotiation n
            join n.resourcesLink rl
            JOIN rl.id.resource r
            JOIN r.networks networks
            where networks.id = :networkId and rl.currentState = 'RESOURCE_MADE_AVAILABLE'
                    and DATE(n.creationDate) > :since and DATE(n.creationDate) <= :until
        """)
                .setParameter("networkId", networkId)
                .setParameter("since", since)
                .setParameter("until", until)
                .getSingleResult())
        .intValue();
  }

  @Override
  public Integer countAllForNetwork(LocalDate since, LocalDate until, Long networkId) {
    return ((Number)
            entityManager
                .createQuery(
                    """
        SELECT COUNT(DISTINCT n.id)
        FROM Negotiation n
        JOIN n.resourcesLink rl
        JOIN rl.resource rs
        JOIN rs.networks net
        WHERE net.id = :networkId
          AND DATE(n.creationDate) > :since
          AND DATE(n.creationDate) <= :until
        """)
                .setParameter("networkId", networkId)
                .setParameter("since", since)
                .setParameter("until", until)
                .getSingleResult())
        .intValue();
  }

  @Override
  public List<Object[]> countStatusDistribution(LocalDate since, LocalDate until, Long networkId) {
    String query =
        """
        SELECT n.currentState, COUNT(DISTINCT n.id)
        FROM Negotiation n
        JOIN n.resourcesLink rl
        JOIN rl.resource rs
        JOIN rs.networks net
        WHERE net.id = :networkId
          AND DATE(n.creationDate) > :since
          AND DATE(n.creationDate) <= :until
        GROUP BY n.currentState
        """;

    return entityManager
        .createQuery(query, Object[].class)
        .setParameter("networkId", networkId)
        .setParameter("since", since)
        .setParameter("until", until)
        .getResultList();
  }

  @Override
  public Integer getNumberOfNewRequesters(LocalDate since, LocalDate until, Long networkId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    """
        SELECT count (distinct n.created_by) FROM Negotiation n
                                         LEFT JOIN negotiation_resource_link nrlr on n.id = nrlr.negotiation_id
            left join resource r on r.id = nrlr.resource_id
                    left join network_resources_link nrl on r.id = nrl.resource_id
                    left join network n2 on n2.id = nrl.network_id
                                         WHERE n.created_by NOT IN (
                                                                     SELECT DISTINCT created_by
                                                                     FROM Negotiation
                                                                     WHERE DATE(creation_date) < :since
                                                                 ) and n2.id = :networkId and DATE(n.creation_date) > :since and DATE(n.creation_date) <= :until

        """)
                .setParameter("networkId", networkId)
                .setParameter("since", since)
                .setParameter("until", until)
                .getSingleResult())
        .intValue();
  }

  @Override
  public Integer getNumberOfActiveRepresentatives(
      LocalDate since, LocalDate until, Long networkId) {
    return ((Number)
            entityManager
                .createNativeQuery(
                    """
        SELECT COUNT(DISTINCT created_by) AS SUM_COUNT
        FROM (
            SELECT nrlr.created_by
            FROM Negotiation n
            LEFT JOIN public.negotiation_resource_lifecycle_record nrlr ON n.id = nrlr.negotiation_id
            LEFT JOIN public.network_resources_link netrl ON nrlr.resource_id = netrl.resource_id
            WHERE DATE(nrlr.creation_date) > :since
              AND DATE(nrlr.creation_date) <= :until
              AND netrl.network_id = :networkId
                AND nrlr.changed_to != 'REPRESENTATIVE_CONTACTED' and nrlr.changed_to != 'REPRESENTATIVE_UNREACHABLE'
            UNION
            SELECT p.created_by
            FROM post p
            LEFT JOIN public.resource_representative_link rrl ON p.created_by = rrl.person_id
            LEFT JOIN public.network_resources_link l ON rrl.resource_id = l.resource_id
            WHERE DATE(p.creation_date) > :since
              AND DATE(p.creation_date) <= :until
              AND l.network_id = :networkId
        ) AS combined;
    """)
                .setParameter("networkId", networkId)
                .setParameter("since", since)
                .setParameter("until", until)
                .getSingleResult())
        .intValue();
  }
}
