package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface RequestRepository extends JpaRepository<Request, String> {

  @NotNull
  @Override
  @EntityGraph(value = "request-with-detailed-resources")
  List<Request> findAll();

  @EntityGraph(value = "request-with-detailed-resources")
  Optional<Request> findDetailedById(String id);
}
