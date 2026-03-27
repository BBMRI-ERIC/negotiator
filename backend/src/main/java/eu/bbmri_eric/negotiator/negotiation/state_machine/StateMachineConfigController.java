package eu.bbmri_eric.negotiator.negotiation.state_machine;

import eu.bbmri_eric.negotiator.negotiation.state_machine.dto.StateMachineConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3/state-machines")
@Tag(name = "State Machines", description = "Create and manage state machine configurations")
@SecurityRequirement(name = "security_auth")
public class StateMachineConfigController {

  private final StateMachineConfigService stateMachineConfigService;

  public StateMachineConfigController(StateMachineConfigService stateMachineConfigService) {
    this.stateMachineConfigService = stateMachineConfigService;
  }

  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Create a new state machine configuration",
      description = "Creates a new state machine with states and transitions atomically")
  public StateMachine createStateMachineConfig(@Valid @RequestBody StateMachineConfigDTO config) {
    return stateMachineConfigService.createStateMachineConfig(config);
  }
}
