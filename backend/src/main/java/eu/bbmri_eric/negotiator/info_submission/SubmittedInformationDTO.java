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
@Schema(description = "DTO representing already submitted information for a resource")
public class SubmittedInformationDTO {

  @NotNull
  @Schema(description = "Unique identifier of the submitted information", example = "456")
  private Long id;

  @NotNull
  @Schema(description = "ID of the resource this information is associated with", example = "123")
  private Long resourceId;

  @Schema(
      description = "ID of the specific requirement this submission fulfills, if applicable",
      example = "789")
  private Long requirementId;

  @NotNull
  @Schema(
      description = "Submitted information as a JSON object",
      example = "{\"field1\": \"value1\"}")
  private JsonNode payload;

  @Schema(
      description = "Flag indicating whether the information is editable",
      example = "true",
      defaultValue = "false")
  private boolean editable;
}
