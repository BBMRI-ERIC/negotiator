package eu.bbmri_eric.negotiator.governance.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for updating an organization")
@ToString
public class OrganizationUpdateDTO {
  @Schema(description = "Name of the organization", example = "BBMRI-ERIC")
  private String name;

  @Schema(
      description = "Description of the organization",
      example = "A European research infrastructure.")
  private String description;

  @Schema(description = "External identifier of the organization", example = "ORG-12345")
  private String externalId;

  @Schema(description = "Contact email of the organization", example = "info@organization.org")
  private String contactEmail;

  @Schema(description = "Indicates if the organization is withdrawn", example = "false")
  private Boolean withdrawn;

  @Schema(description = "URI of the organization", example = "https://organization.org")
  private String uri;
}
