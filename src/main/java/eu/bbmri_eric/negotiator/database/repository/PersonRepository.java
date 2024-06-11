package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
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
public interface PersonRepository
    extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

  Optional<Person> findByName(String authName);

  Page<Person> findAllByName(String name, Pageable pageable);

  Optional<Person> findBySubjectId(String authSubject);

  Optional<Person> findDetailedById(Long id);

  List<Person> findAllByAdminIsTrue();

  boolean existsByIdAndResourcesIn(Long id, Set<Resource> resources);

  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT n.id "
              + "FROM negotiation n "
              + "WHERE n.id = :negotiationId AND "
              + "n.created_by = :personId)",
      nativeQuery = true)
  boolean isNegotiationCreator(Long personId, String negotiationId);

  @Query(
      value =
          "SELECT EXISTS (SELECT rs.id "
              + "FROM request rq JOIN request_resources_link rrl on rq.id = rrl.request_id "
              + "                JOIN resource rs on rs.id = rrl.resource_id "
              + "WHERE rq.negotiation_id = :negotiationId AND "
              + "      rs.id in ("
              + "         select rrl.resource_id "
              + "         from person p join resource_representative_link rrl ON p.id = rrl.person_id "
              + "         where p.id = :personId"
              + "))",
      nativeQuery = true)
  boolean isRepresentativeOfAnyResourceOfNegotiation(Long personId, String negotiationId);

  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT r.id "
              + "FROM resource r "
              + "WHERE organization_id = :organizationId and  r.id IN ("
              + "      SELECT rrl.resource_id "
              + "      FROM person p JOIN resource_representative_link rrl ON p.id = rrl.person_id "
              + "      WHERE p.id = :personId "
              + "))",
      nativeQuery = true)
  boolean isRepresentativeOfAnyResourceOfOrganization(Long personId, Long organizationId);

  @Query(
      value =
          "SELECT EXISTS ("
              + "SELECT r.id "
              + "FROM resource r "
              + "JOIN organization o ON r.organization_id = o.id "
              + "WHERE o.external_id = :organizationExternalId and r.id IN ("
              + "      SELECT rrl.resource_id "
              + "      FROM person p JOIN resource_representative_link rrl ON p.id = rrl.person_id "
              + "      WHERE p.id = :personId "
              + "))",
      nativeQuery = true)
  boolean isRepresentativeOfAnyResourceOfOrganization(Long personId, String organizationExternalId);

  Page<Person> findAllByNetworksContains(Network network, Pageable pageable);
}
