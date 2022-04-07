package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {}