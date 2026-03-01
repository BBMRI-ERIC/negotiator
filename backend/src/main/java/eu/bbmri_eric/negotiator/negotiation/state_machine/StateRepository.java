package eu.bbmri_eric.negotiator.negotiation.state_machine;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, Long> {
  List<State> findByStateMachineId(Long stateMachineId);
}
