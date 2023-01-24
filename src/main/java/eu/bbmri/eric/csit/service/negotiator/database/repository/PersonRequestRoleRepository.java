package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRequestRoleRepository extends JpaRepository<PersonNegotiationRole, Long> {

}
