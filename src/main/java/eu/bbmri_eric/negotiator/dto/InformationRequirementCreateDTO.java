package eu.bbmri_eric.negotiator.dto;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.InformationRequirement} */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InformationRequirementCreateDTO {
  @NotNull(message = "requiredAccessFormId must not be null")
  private Long requiredAccessFormId;

  @NotNull(message = "forResourceEvent must not be null")
  private NegotiationResourceEvent forResourceEvent;
}
