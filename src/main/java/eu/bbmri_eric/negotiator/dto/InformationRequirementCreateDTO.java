package eu.bbmri_eric.negotiator.dto;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.InformationRequirement} */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InformationRequirementCreateDTO {
  private Long requiredAccessFormId;
  private NegotiationResourceEvent forResourceEvent;
}
