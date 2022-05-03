package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Biobank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiobankRepository extends JpaRepository<Biobank, Long> {}
