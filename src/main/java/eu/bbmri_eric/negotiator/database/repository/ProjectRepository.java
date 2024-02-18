package eu.bbmri_eric.negotiator.database.repository;

import eu.bbmri_eric.negotiator.database.model.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

  @Override
  @EntityGraph(value = "project-detailed")
  List<Project> findAll();

  @EntityGraph(value = "project-detailed")
  Optional<Project> findDetailedById(String id);
}
