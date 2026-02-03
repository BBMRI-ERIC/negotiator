package eu.bbmri_eric.negotiator.negotiation.state_machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.statemachine.config.model.ConfigurationData;
import org.springframework.statemachine.config.model.DefaultStateMachineModel;
import org.springframework.statemachine.config.model.StateData;
import org.springframework.statemachine.config.model.StateMachineModel;
import org.springframework.statemachine.config.model.StateMachineModelFactory;
import org.springframework.statemachine.config.model.StatesData;
import org.springframework.statemachine.config.model.TransitionData;
import org.springframework.statemachine.config.model.TransitionsData;
import org.springframework.stereotype.Component;

@Component
public class DBStateMachineFactory implements StateMachineModelFactory<String, String> {

  private final StateMachineRepository stateMachineRepository;
  private final StateRepository stateRepository;
  private final TransitionRepository transitionRepository;

  public DBStateMachineFactory(
      StateMachineRepository stateMachineRepository,
      StateRepository stateRepository,
      TransitionRepository transitionRepository) {
    this.stateMachineRepository = stateMachineRepository;
    this.stateRepository = stateRepository;
    this.transitionRepository = transitionRepository;
  }

  @Override
  public StateMachineModel<String, String> build() {
    // Build a default state machine model
    return build(null);
  }

  @Override
  public StateMachineModel<String, String> build(String machineId) {
    StateMachine stateMachine = null;
    if (machineId != null) {
      stateMachine = stateMachineRepository.findByName(machineId).orElse(null);
    }

    if (stateMachine == null) {
      return new DefaultStateMachineModel<>(
          new ConfigurationData<>(),
          new StatesData<>(new ArrayList<>()),
          new TransitionsData<>(new ArrayList<>()));
    }
    List<State> states = stateRepository.findByStateMachineId(stateMachine.getId());
    List<Transition> transitions = transitionRepository.findAll();
    Collection<StateData<String, String>> stateDataCollection = new ArrayList<>();
    Map<Long, String> stateIdToNameMap = new HashMap<>();

    for (State state : states) {
      stateIdToNameMap.put(state.getId(), state.getName());
      boolean isInitial = state.getType() == StateType.START;
      StateData<String, String> stateData = new StateData<>(state.getName(), isInitial);
      stateData.setEnd(state.getType().equals(StateType.END));
      stateDataCollection.add(stateData);
    }

    StatesData<String, String> statesData = new StatesData<>(stateDataCollection);
    Collection<TransitionData<String, String>> transitionDataCollection = new ArrayList<>();
    for (Transition transition : transitions) {
      String sourceName = stateIdToNameMap.get(transition.getSource());
      String targetName = stateIdToNameMap.get(transition.getTarget());

      if (sourceName != null && targetName != null) {
        // TransitionData(source, target, state, event, period, count, actions, guard, kind,
        // securityRule, name)
        TransitionData<String, String> transitionData =
            new TransitionData<>(sourceName, targetName, transition.getName());
        transitionDataCollection.add(transitionData);
      }
    }

    TransitionsData<String, String> transitionsData =
        new TransitionsData<>(transitionDataCollection);

    // Create configuration data
    ConfigurationData<String, String> configurationData = new ConfigurationData<>();

    return new DefaultStateMachineModel<>(configurationData, statesData, transitionsData);
  }
}
