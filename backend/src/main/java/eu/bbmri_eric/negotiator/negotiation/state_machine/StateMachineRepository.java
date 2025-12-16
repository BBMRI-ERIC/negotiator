package eu.bbmri_eric.negotiator.negotiation.state_machine;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateMachineRepository extends JpaRepository<StateMachine, Long> {
    Optional<StateMachine> findByName(String name);
}

