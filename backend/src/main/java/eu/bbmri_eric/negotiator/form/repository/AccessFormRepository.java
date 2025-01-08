package eu.bbmri_eric.negotiator.form.repository;

import eu.bbmri_eric.negotiator.form.AccessForm;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessFormRepository extends JpaRepository<AccessForm, Long> {

  Optional<AccessForm> findById(Long id);

  @Query(
      value =
          "SELECT DISTINCT a "
              + "FROM AccessForm a "
              + "JOIN FETCH a.resources r "
              + "WHERE r.sourceId = :entityId")
  Optional<AccessForm> findByResourceId(String entityId);
}
