package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

  Optional<Person> findByAuthNameAndPasswordNotNull(String authName);

  Optional<Person> findByAuthName(String authName);

  Optional<Person> findByAuthSubject(String authSubject);

  @EntityGraph(value = "person-detailed")
  Optional<Person> findDetailedById(Long id);

  Optional<Person> deleteByAuthSubject(String authSubject);
}
