package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {

  @Override
  @EntityGraph(value = "project-detailed")
  List<Project> findAll();

  @EntityGraph(value = "project-detailed")
  Optional<Project> findDetailedById(String id);
}
