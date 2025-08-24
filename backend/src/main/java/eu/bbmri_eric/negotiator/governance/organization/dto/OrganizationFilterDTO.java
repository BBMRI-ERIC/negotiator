package eu.bbmri_eric.negotiator.governance.organization.dto;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Filter for organizations")
public class OrganizationFilterDTO implements FilterDTO {

  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  private int page = 0;

  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  private int size = 50;

  @Schema(description = "Filter organizations by name", example = "BBMRI-ERIC")
  private String name;

  @Schema(description = "Filter organizations by external ID", example = "biobank:1")
  private String externalId;

  @Schema(description = "Filter organizations by withdrawn status", example = "false")
  private Boolean withdrawn;
}
