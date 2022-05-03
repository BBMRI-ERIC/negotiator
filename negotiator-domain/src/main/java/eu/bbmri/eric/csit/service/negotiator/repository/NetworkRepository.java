package eu.bbmri.eric.csit.service.negotiator.repository;

import eu.bbmri.eric.csit.service.negotiator.model.Network;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NetworkRepository extends JpaRepository<Network, Long> {}
