package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository
    extends JpaRepository<Person, Long>, JpaSpecificationExecutor<Person> {

  Optional<Person> findByName(String authName);

  Page<Person> findAllByName(String name, Pageable pageable);

  Optional<Person> findBySubjectId(String authSubject);

  @EntityGraph(value = "person-detailed")
  Optional<Person> findDetailedById(Long id);

  @EntityGraph(value = "person-detailed")
  List<Person> findAllByAdminIsTrue();

  @EntityGraph(value = "person-detailed")
  List<Person> findAllByResourcesIn(Set<Resource> resources);

  @EntityGraph(value = "person-detailed")
  boolean existsByIdAndResourcesIn(Long id, Set<Resource> resources);
}
