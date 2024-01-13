package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NegotiationRepository extends JpaRepository<Negotiation, String> {

  @Override
  @EntityGraph(value = "negotiation-with-detailed-children")
  List<Negotiation> findAll();

  @EntityGraph(value = "negotiation-with-detailed-children")
  Optional<Negotiation> findDetailedById(String id);

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
              + " AND n.currentState = :currentState")
  List<Negotiation> findByResourceExternalIdsAndCurrentState(
      List<String> collectionIds, NegotiationState currentState);

  @EntityGraph(value = "negotiation-with-detailed-children")
  List<Negotiation> findByCreatedBy_Id(Long personId);

  @EntityGraph(value = "negotiation-with-detailed-children")
  List<Negotiation> findByCurrentState(NegotiationState negotiationState);

  boolean existsByIdAndCreatedBy_Id(String negotiationId, Long personId);
}
