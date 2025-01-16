package eu.bbmri_eric.negotiator.governance.organization;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "organizations", itemRelation = "organization")
@Schema(description = "An organization")
public class OrganizationDTO {
  @NotNull
  @Schema(description = "Unique identifier of the organization", example = "1")
  private Long id;

  @NotNull
  @Schema(description = "External identifier of the organization", example = "ORG-12345")
  private String externalId;

  @NotNull
  @Schema(description = "Name of the organization", example = "BBMRI-ERIC")
  private String name;

  @Schema(
      description = "Description of the organization",
      example = "A European research infrastructure.")
  private String description;

  @Schema(description = "Contact email of the organization", example = "info@organization.org")
  private String contactEmail;

  @Schema(description = "URI of the organization", example = "https://organization.org")
  private String uri;

  @Schema(description = "Indicates if the organization is withdrawn", example = "false")
  private Boolean withdrawn;
}
