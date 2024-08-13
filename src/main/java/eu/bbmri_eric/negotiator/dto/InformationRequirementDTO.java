package eu.bbmri_eric.negotiator.dto;

import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceEvent;
import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
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
