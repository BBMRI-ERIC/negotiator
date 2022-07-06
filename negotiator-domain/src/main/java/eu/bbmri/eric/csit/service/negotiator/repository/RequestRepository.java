package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Request;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

  @Override
  @EntityGraph(value = "request-with-detailed-children")
  List<Request> findAll();

  @EntityGraph(value = "request-with-detailed-children")
  Optional<Request> findDetailedById(Long id);

  Optional<Request> findByToken(String token);

  @Query(
      value =
          "SELECT DISTINCT r "
              + "FROM Request r "
              + "JOIN FETCH r.persons pp "
              + "JOIN FETCH pp.person "
              + "JOIN FETCH pp.role "
              + "JOIN FETCH r.project p "
              + "JOIN FETCH r.queries q "
              + "JOIN FETCH q.collections c "
              + "JOIN FETCH c.biobank b "
              + "WHERE b.sourceId = :biobankId")
  List<Request> findByBiobankId(String biobankId);

  @Query(
      value =
          "SELECT DISTINCT r "
              + "FROM Request r "
              + "JOIN FETCH r.persons pp "
              + "JOIN FETCH pp.person "
              + "JOIN FETCH pp.role "
              + "JOIN FETCH r.project p "
              + "JOIN FETCH r.queries q "
              + "JOIN FETCH q.collections c "
              + "JOIN FETCH c.biobank b "
              + "WHERE c.sourceId = :collectionId")
  List<Request> findByCollectionId(String collectionId);
}
