package eu.bbmri_eric.negotiator.info_requirement;

import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Relation(collectionRelation = "info-requirements", itemRelation = "info-requirement")
public class InformationRequirementDTO {
  private Long id;
  private AccessFormDTO requiredAccessForm;
  private NegotiationResourceEvent forResourceEvent;
  private boolean isViewableOnlyByAdmin;
}
