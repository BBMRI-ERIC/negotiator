package eu.bbmri.eric.csit.service.negotiator.database.repository;

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

  @Query(value = "SELECT DISTINCT r "
      + "FROM Negotiation r "
      + "JOIN FETCH r.persons pp "
      + "JOIN FETCH pp.person p "
      + "JOIN FETCH pp.role role "
      + "where p.authSubject = :userId and role.name = :userRole")
  List<Negotiation> findByUserIdAndRole(@Param("userId") String userId,
      @Param("userRole") String userRole);

  @Query(
      value =
          "SELECT r "
              + "FROM Negotiation r "
              + "JOIN FETCH r.persons pp "
              + "JOIN FETCH pp.person "
              + "JOIN FETCH pp.role "
              + "JOIN FETCH r.requests rr "
              + "JOIN FETCH rr.resources c "
              + "JOIN FETCH c.parent p "
              + "WHERE p.id = :biobankId")
  List<Negotiation> findByBiobankId(String biobankId);

  @Query(
      value =
          "SELECT DISTINCT r "
              + "FROM Negotiation r "
              + "JOIN FETCH r.persons pp "
              + "JOIN FETCH pp.person "
              + "JOIN FETCH pp.role "
              + "JOIN FETCH r.requests rr "
              + "JOIN FETCH rr.resources c "
              + "JOIN FETCH c.parent p "
              + "WHERE c.sourceId = :collectionId")
  List<Negotiation> findByCollectionId(String collectionId);
}
