package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Biobank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BiobankRepository extends JpaRepository<Biobank, Long> {}
