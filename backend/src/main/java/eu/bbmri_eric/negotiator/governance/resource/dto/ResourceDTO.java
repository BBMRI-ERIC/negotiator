package eu.bbmri_eric.negotiator.governance.resource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

/** A DTO depicting an abstract resource coming in a request from a Discovery Service */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Deprecated
public class ResourceDTO {
  @Schema(
      description = "A unique Identifier of the Resource.",
      example = "bbmri-eric:ID:CZ_MMCI:collection:LTS")
  @NotNull
  private String id;

  @Schema(
      nullable = true,
      description = "Name of the Resource",
      example = "A collection of bio samples at MMCI")
  @Nullable
  private String name;

  @Nullable private String description;

  @Nullable private String contactEmail;

  @Nullable private String uri;

  @Schema(nullable = true, description = "Organization Managing the Resource", example = "{}")
  private OrganizationDTO organization;
}
