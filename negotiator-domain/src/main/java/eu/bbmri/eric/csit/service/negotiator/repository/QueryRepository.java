package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Query;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface QueryRepository extends JpaRepository<Query, Long> {

  @NotNull
  @Override
  @EntityGraph(value = "query-with-detailed-resources")
  List<Query> findAll();

  @EntityGraph(value = "query-with-detailed-resources")
  Optional<Query> findDetailedById(Long id);

  //  @EntityGraph(value = "query-with-detailed-collections")
  Optional<Query> findByToken(String token);
}
