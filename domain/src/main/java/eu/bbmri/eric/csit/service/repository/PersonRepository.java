package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {}
