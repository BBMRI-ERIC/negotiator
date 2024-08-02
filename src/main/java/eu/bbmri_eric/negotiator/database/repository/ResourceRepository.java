package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.model.views.ResourceViewDTO;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

  @Query(
      value =
          "SELECT rs.name as name, rs.source_id as sourceId, rspn.current_state as currentState, o.name as organizationName, o.external_id as organizationExternalId "
              + "FROM resource rs join resource_state_per_negotiation rspn on rs.source_id = rspn.resource_id "
              + "join organization o on o.id = rs.organization_id "
              + "where rspn.negotiation_id = :negotiationId",
      nativeQuery = true)
  List<ResourceViewDTO> findByNegotiation(String negotiationId);

  Optional<Resource> findByName(String name);

  Optional<Resource> findBySourceId(String sourceId);

  List<Resource> findAllBySourceIdIn(Set<String> sourceIds);

  Page<Resource> findAllByNetworksContains(Network network, Pageable pageable);
}
