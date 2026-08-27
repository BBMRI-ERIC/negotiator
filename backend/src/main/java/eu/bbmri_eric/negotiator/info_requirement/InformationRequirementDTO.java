package eu.bbmri_eric.negotiator.info_requirement;

import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "Event guarded by this information requirement", example = "CONTACT")
  private String forResourceEvent;

  private boolean isViewableOnlyByAdmin;
}
