package eu.bbmri_eric.negotiator.governance.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "DTO for creating an organization.")
public class OrganizationCreateDTO {

  @NotNull
  @Schema(description = "Name of the organization", example = "BBMRI-ERIC")
  private String name;

  @NotNull
  @Schema(
      description = "Description of the organization",
      example = "A European research infrastructure.")
  private String description;

  @NotNull
  @Schema(description = "External identifier of the organization", example = "ORG-12345")
  private String externalId;

  @Schema(description = "Contact email of the organization", example = "info@organization.org")
  private String contactEmail;

  @NotNull
  @Builder.Default
  @Schema(description = "Indicates if the organization is withdrawn", example = "false")
  private Boolean withdrawn = false;

  @Schema(description = "URI of the organization", example = "https://organization.org")
  private String uri;
}
