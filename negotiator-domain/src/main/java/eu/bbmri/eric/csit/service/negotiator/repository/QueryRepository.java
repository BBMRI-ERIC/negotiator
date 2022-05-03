package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryRepository extends JpaRepository<Query, Long> {}
