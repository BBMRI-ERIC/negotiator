package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

  Optional<Role> findByName(String name);
}
