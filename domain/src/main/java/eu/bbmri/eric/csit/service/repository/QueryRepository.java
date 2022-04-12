package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {}
