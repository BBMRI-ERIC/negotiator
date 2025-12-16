package eu.bbmri_eric.negotiator.negotiation.state_machine;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransitionRepository extends JpaRepository<Transition, Long> {
}

