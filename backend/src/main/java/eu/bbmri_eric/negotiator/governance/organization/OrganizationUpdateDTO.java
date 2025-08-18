package eu.bbmri_eric.negotiator.governance.organization;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO for updating an organization")
@ToString
public class OrganizationUpdateDTO {
  @Schema(description = "Name of the organization", example = "BBMRI-ERIC")
  @Size(max = 255, message = "Organization name must not exceed 255 characters")
  private String name;

  @Schema(
      description = "Description of the organization",
      example = "A European research infrastructure.")
  @Size(max = 5000, message = "Description must not exceed 5000 characters")
  private String description;

  @Schema(description = "External identifier of the organization", example = "ORG-12345")
  @Size(max = 255, message = "External ID must not exceed 255 characters")
  private String externalId;

  @Schema(description = "Contact email of the organization", example = "info@organization.org")
  @Email(message = "Contact email must be a valid email address")
  @Size(max = 255, message = "Contact email must not exceed 255 characters")
  private String contactEmail;

  @Schema(description = "Indicates if the organization is withdrawn", example = "false")
  private Boolean withdrawn;

  @Schema(description = "URI of the organization", example = "https://organization.org")
  @URL(message = "URI must be a valid URL")
  @Size(max = 1024, message = "URI must not exceed 1024 characters")
  private String uri;
}
