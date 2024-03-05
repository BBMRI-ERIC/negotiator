package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NegotiationRepository extends JpaRepository<Negotiation, String> {

  @Override
  List<Negotiation> findAll();

  Page<Negotiation> findAllByCreatedBy(Pageable pageable, Person author);

  Optional<Negotiation> findDetailedById(String id);

  Page<Negotiation> findAllByCurrentState(Pageable pageRequest, NegotiationState currentState);

  @Query(
      value =
          "SELECT DISTINCT r "
              + "FROM Negotiation r "
              + "JOIN FETCH r.persons pp "
              + "JOIN FETCH pp.person p "
              + "JOIN FETCH pp.role role "
              + "where p.subjectId = :userId and role.name = :userRole")
  List<Negotiation> findBySubjectIdAndRole(
      @Param("userId") String userId, @Param("userRole") String userRole);

  @Query(
      value =
          "SELECT DISTINCT r "
              + "FROM Negotiation r "
              + "JOIN FETCH r.requests rr "
              + "JOIN FETCH rr.resources c "
              + "JOIN FETCH r.persons p "
              + "WHERE c.sourceId = :collectionId")
  List<Negotiation> findByCollectionId(String collectionId);

  @Query(
      value =
          "SELECT DISTINCT r "
              + "FROM Negotiation r "
              + "JOIN FETCH r.requests rr "
              + "JOIN FETCH rr.resources c "
              + "JOIN FETCH r.persons pp "
              + "JOIN FETCH pp.person "
              + "JOIN FETCH pp.role "
              + "WHERE c.sourceId IN :collectionIds")
  List<Negotiation> findByCollectionIds(List<String> collectionIds);

  @Query(
      value =
          "SELECT DISTINCT n "
              + "FROM Negotiation n "
              + "JOIN FETCH n.requests rr "
              + "JOIN FETCH rr.resources c "
              + "JOIN FETCH n.persons pp "
              + "JOIN FETCH pp.person p "
              + "JOIN FETCH pp.role role "
              + "WHERE c.sourceId IN :collectionIds"
              + " AND n.currentState IN :currentStates")
  Page<Negotiation> findByResourceExternalIdsAndCurrentState(
      Pageable pageable, List<String> collectionIds, List<NegotiationState> currentStates);

  Page<Negotiation> findByCreatedByOrRequests_ResourcesIn(
      Pageable pageable, Person person, Set<Resource> resources);

  List<Negotiation> findByCreatedBy_Id(Long personId);

  List<Negotiation> findByCurrentState(NegotiationState negotiationState);

  boolean existsByIdAndCreatedBy_Id(String negotiationId, Long personId);
}
