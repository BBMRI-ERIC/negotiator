package eu.bbmri_eric.negotiator.governance.network;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@ToString
@Schema(description = "DTO for creating a network.")
public class NetworkCreateDTO {
  @NotNull
  @Schema(description = "External identifier of the network", example = "NET-98765")
  private String externalId;

  @NotNull
  @Schema(description = "Name of the network", example = "European Bioinformatics Network")
  private String name;

  @Schema(
      description = "Description of the network",
      example = "A collaborative network for bioinformatics research.")
  private String description;

  @Schema(description = "Contact email for the network", example = "info@network.org")
  private String contactEmail;

  @Schema(description = "URI of the network", example = "https://network.org")
  private String uri;
}
