package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
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
          "select exists ( "
              + "select nrl.resource_id "
              + "from negotiation_resources_link nrl "
              + "where nrl.negotiation_id = :negotiationId and "
              + "   nrl.resource_id in ( "
              + "       select rrl.resource_id "
              + "       from person p join resource_representative_link rrl ON p.id = rrl.person_id "
              + "       where p.id = :personId"
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
