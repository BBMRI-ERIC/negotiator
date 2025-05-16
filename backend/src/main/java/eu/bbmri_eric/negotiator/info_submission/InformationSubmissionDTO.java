package eu.bbmri_eric.negotiator.info_submission;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "DTO representing an information submission for a resource")
public class InformationSubmissionDTO {

  @NotNull
  @Schema(description = "ID of the resource the information is submitted for", example = "123")
  private Long resourceId;

  @NotNull
  @Schema(
      description = "Payload containing the submitted information as a JSON object",
      example = "{\"key\": \"value\"}")
  private JsonNode payload;

  @Schema(
      description = "Flag indicating whether the information has already been submitted",
      example = "false",
      defaultValue = "false")
  private boolean submitted = false;

  public InformationSubmissionDTO(Long resourceId, JsonNode payload) {
    this.resourceId = resourceId;
    this.payload = payload;
  }
}
