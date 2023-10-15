package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Request;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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
