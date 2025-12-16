package eu.bbmri_eric.negotiator.negotiation.state_machine.dto;

import java.util.List;

public record StateMachineConfigDTO(
        String name,
        List<StateDTO> states,
        List<TransitionDTO> transitions
) {
}

