package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {}
