package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Request;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
  @Query(
      value =
          "SELECT DISTINCT r from Request r JOIN r.queries q JOIN q.collections c JOIN c.biobank b WHERE b.sourceId = :biobankId")
  List<Request> findByBiobankId(String biobankId);

  @Query(
      value =
          "SELECT DISTINCT r from Request r JOIN r.queries q JOIN q.collections c WHERE c.sourceId = :collectionId")
  List<Request> findByCollectionId(String collectionId);
}
