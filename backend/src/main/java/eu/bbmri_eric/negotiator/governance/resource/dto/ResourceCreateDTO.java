package eu.bbmri_eric.negotiator.governance.resource.dto;

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
@Schema(description = "DTO for creating a resource.")
public class ResourceCreateDTO {

  @NotNull
  @Schema(description = "Name of the resource", example = "Clinical Data Repository")
  private String name;

  @Schema(description = "Description of the resource", example = "A repository for clinical data.")
  private String description;

  @NotNull
  @Schema(description = "Source identifier of the resource", example = "SRC-56789")
  private String sourceId;

  @NotNull
  @Schema(description = "ID of the associated organization", example = "1")
  private Long organizationId;

  @NotNull
  @Schema(description = "ID of the access form related to the resource", example = "42")
  private Long accessFormId;

  @NotNull
  @Schema(description = "ID of the discovery service associated with the resource", example = "10")
  private Long discoveryServiceId;

  @Schema(description = "Contact email for the resource", example = "support@resource.org")
  private String contactEmail;

  @Schema(description = "URI of the resource", example = "https://resource.org")
  private String uri;
}
