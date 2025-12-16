package eu.bbmri_eric.negotiator.negotiation.state_machine;

import eu.bbmri_eric.negotiator.negotiation.state_machine.dto.StateMachineConfigDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.dto.TransitionDTO;
import jakarta.transaction.Transactional;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class StateMachineConfigService {

    private final StateMachineRepository stateMachineRepository;
    private final StateRepository stateRepository;
    private final TransitionRepository transitionRepository;

    public StateMachineConfigService(
            StateMachineRepository stateMachineRepository,
            StateRepository stateRepository,
            TransitionRepository transitionRepository) {
        this.stateMachineRepository = stateMachineRepository;
        this.stateRepository = stateRepository;
        this.transitionRepository = transitionRepository;
    }

    @Transactional
    public StateMachine createStateMachineConfig(StateMachineConfigDTO config) {
        if (stateMachineRepository.findByName(config.name()).isPresent()) {
            throw new IllegalArgumentException("State machine with name '" + config.name() + "' already exists");
        }

        StateMachine stateMachine = new StateMachine();
        stateMachine.setName(config.name());
        stateMachine = stateMachineRepository.save(stateMachine);

        Map<String, State> stateMap = new HashMap<>();
        for (var stateDTO : config.states()) {
            State state = new State();
            state.setStateMachineId(stateMachine.getId());
            state.setName(stateDTO.name());
            state.setType(stateDTO.type());
            state = stateRepository.save(state);
            stateMap.put(state.getName(), state);
        }

        for (TransitionDTO transitionDTO : config.transitions()) {
            State sourceState = stateMap.get(transitionDTO.sourceName());
            State targetState = stateMap.get(transitionDTO.targetName());

            if (sourceState == null) {
                throw new IllegalArgumentException("Source state '" + transitionDTO.sourceName() + "' not found");
            }
            if (targetState == null) {
                throw new IllegalArgumentException("Target state '" + transitionDTO.targetName() + "' not found");
            }

            Transition transition = new Transition();
            transition.setSource(sourceState.getId());
            transition.setTarget(targetState.getId());
            transition.setName(transitionDTO.eventName());
            transitionRepository.save(transition);
        }

        return stateMachine;
    }
}

