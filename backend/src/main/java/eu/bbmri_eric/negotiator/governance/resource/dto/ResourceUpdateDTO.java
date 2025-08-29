package eu.bbmri_eric.negotiator.governance.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for updating a resource.")
public class ResourceUpdateDTO {

  @Schema(
      description = "Updated name of the resource",
      example = "Updated Clinical Data Repository")
  private String name;

  @Schema(
      description = "Updated description of the resource",
      example = "An updated description for the clinical data repository.")
  private String description;

  @Schema(
      description = "Updated contact email for the resource",
      example = "new-support@resource.org")
  private String contactEmail;

  @Schema(description = "If the Resource is still active or not", example = "false")
  private boolean withdrawn;

  @Schema(description = "Updated URI of the resource", example = "https://updated.resource.org")
  private String uri;
}
