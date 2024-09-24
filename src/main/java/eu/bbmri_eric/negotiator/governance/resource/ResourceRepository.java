package eu.bbmri_eric.negotiator.governance.resource;

import eu.bbmri_eric.negotiator.governance.network.Network;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResourceRepository
    extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

  @Query(
      value =
          """
select
    rs.id              as id,
    nrl.negotiation_id as negotiationId,
    rs.name            as name,
    rs.source_id       as sourceId,
    rspn.current_state as currentState,
    o.name             as organizationName,
    o.external_id      as organizationExternalId,
    o.id               as organizationId
from resource rs
    join public.negotiation_resources_link nrl on rs.id = nrl.resource_id
    join public.organization o on o.id = rs.organization_id
    left join public.resource_state_per_negotiation rspn on rs.source_id = rspn.resource_id and nrl.negotiation_id = rspn.negotiation_id
where
    nrl.negotiation_id = :negotiationId
order by rs.source_id;
""",
      nativeQuery = true)
  List<ResourceViewDTO> findByNegotiation(String negotiationId);

  Optional<Resource> findByName(String name);

  Optional<Resource> findBySourceId(String sourceId);

  List<Resource> findAllBySourceIdIn(Set<String> sourceIds);

  Page<Resource> findAllByNetworksContains(Network network, Pageable pageable);
}
