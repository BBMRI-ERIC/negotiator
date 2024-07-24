package eu.bbmri_eric.negotiator.dto;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.InformationRequirement} */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformationRequirementCreateDTO implements Serializable {
  @NotNull private Long requiredAccessFormId;
  private @NotNull NegotiationResourceEvent forResourceState;
}
