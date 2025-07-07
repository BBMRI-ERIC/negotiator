package eu.bbmri_eric.negotiator.user;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PersonRepository
        extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

    Optional<Person> findByName(String authName);

    Page<Person> findAllByName(String name, Pageable pageable);

    Optional<Person> findBySubjectId(String authSubject);

    Optional<Person> findDetailedById(Long id);

    List<Person> findAllByAdminIsTrue();

    List<Person> findAllByAdminIsFalse();

    @Query("""
            select distinct p from Person p join p.resources r where r.organization.id = :id
            """)
    List<Person> findAllByOrganizationId(Long id);

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
                            + "from negotiation_resource_link nrl "
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


    List<Person> findAllByResourcesIs(Long organizationId);

    @Query(
            value =
                    """
                                SELECT COUNT(n) > 0
                                FROM Person p
                                JOIN p.networks n
                                WHERE p.id = :managerId AND n.id = :networkId
                            """)
    boolean isNetworkManager(Long managerId, Long networkId);

    @Query(
            value =
                    """
                            SELECT CASE WHEN EXISTS (
                                SELECT 1
                                FROM negotiation_resource_link nrl
                                WHERE nrl.negotiation_id = :negotiationId
                                  AND nrl.resource_id IN (
                                      SELECT network_link.resource_id
                                      FROM person p
                                      JOIN public.network_person_link npl ON p.id = npl.person_id
                                      JOIN network_resources_link network_link ON network_link.resource_id = nrl.resource_id
                                      WHERE p.id = :personId
                                  )
                            ) THEN TRUE ELSE FALSE END
                            """,
            nativeQuery = true)
    boolean isManagerOfAnyResourceOfNegotiation(Long personId, String negotiationId);

    Page<Person> findAllByNetworksContains(Network network, Pageable pageable);

    List<Person> findAllByResourcesNotEmpty();
}
