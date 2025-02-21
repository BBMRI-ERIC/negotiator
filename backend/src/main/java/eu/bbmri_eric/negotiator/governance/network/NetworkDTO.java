package eu.bbmri_eric.negotiator.governance.network;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Relation(collectionRelation = "networks", itemRelation = "network")
@Schema(description = "A Network grouping together various resources")
public class NetworkDTO {
  @NotNull
  @Schema(description = "Unique identifier of the network", example = "1")
  private Long id;

  @NotNull
  @Schema(description = "External identifier of the network", example = "NET-98765")
  private String externalId;

  @NotNull
  @Schema(description = "Name of the network", example = "European Bioinformatics Network")
  private String name;

  @NotNull
  @Schema(
      description = "Description of the network",
      example = "A collaborative network for bioinformatics research.")
  private String description;

  @Schema(description = "Contact email for the network", example = "info@network.org")
  private String contactEmail;

  @Schema(description = "URI of the network", example = "https://network.org")
  private String uri;
}
