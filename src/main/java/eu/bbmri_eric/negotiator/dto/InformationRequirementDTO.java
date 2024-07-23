package eu.bbmri_eric.negotiator.dto;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link eu.bbmri_eric.negotiator.database.model.InformationRequirement} */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class InformationRequirementDTO implements Serializable {
  private Long id;
  private AccessFormDTO requiredAccessForm;
  private NegotiationResourceState forResourceState;
}
