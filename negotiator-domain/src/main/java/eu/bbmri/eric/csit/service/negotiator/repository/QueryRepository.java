package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Query;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface QueryRepository extends JpaRepository<Query, Long> {

  @org.springframework.data.jpa.repository.Query(
      value = "SELECT q from Query q JOIN FETCH q.collections c JOIN FETCH c.biobank")
  List<Query> findDetailedAll();

  @org.springframework.data.jpa.repository.Query(
      value =
          "SELECT q from Query q JOIN FETCH q.collections c JOIN FETCH c.biobank WHERE q.id = :id")
  Optional<Query> findDetailedById(Long id);
}
