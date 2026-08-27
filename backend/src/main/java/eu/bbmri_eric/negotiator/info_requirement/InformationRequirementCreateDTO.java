package eu.bbmri_eric.negotiator.info_requirement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceEventNameDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for {@link eu.bbmri_eric.negotiator.info_requirement.InformationRequirement} */
@Data
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InformationRequirementCreateDTO {

  public InformationRequirementCreateDTO(Long requiredAccessFormId, String forResourceEvent) {
    this.requiredAccessFormId = requiredAccessFormId;
    this.forResourceEvent = forResourceEvent;
  }

  @NotNull(message = "requiredAccessFormId must not be null")
  private Long requiredAccessFormId;

  @NotNull(message = "forResourceEvent must not be null")
  @Schema(description = "Event guarded by this information requirement", example = "CONTACT")
  @JsonDeserialize(using = NegotiationResourceEventNameDeserializer.class)
  private String forResourceEvent;

  private boolean isViewableOnlyByAdmin = true;
}
