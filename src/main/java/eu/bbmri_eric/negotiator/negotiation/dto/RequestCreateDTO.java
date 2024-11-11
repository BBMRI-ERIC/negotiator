package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class RequestCreateDTO {

  @NotNull(message = "The URL of the Discovery Service must be present")
  @Schema(description = "URL of the Discovery Service", example = "https://bbmritestnn.gcc.rug.nl")
  private String url;

  @NotNull(message = "A human readable description of the request must be present")
  @Schema(
      description =
          "A human readable description of the request such as filters used in the Discovery Service",
      example = "#1: No filters used")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<ResourceDTO> resources;

  @Override
  public String toString() {
    return "RequestCreateDTO{url='%s',\n humanReadable='%s',\nresources=%s}"
        .formatted(url, humanReadable, resources);
  }
}
