package eu.bbmri.eric.csit.service.repository;

import eu.bbmri.eric.csit.service.model.Network;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkRepository extends JpaRepository<Network, Long> {}
