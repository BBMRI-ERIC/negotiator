package eu.bbmri_eric.negotiator.governance.resource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** A DTO for an abstract resource in the Negotiator. */
@Getter
@Setter
@Relation(collectionRelation = "resources", itemRelation = "resource")
@NoArgsConstructor
@Schema(description = "A DTO representing an abstract resource in the Negotiator.")
public class ResourceResponseModel {
  @Schema(description = "Unique identifier of the resource", example = "1")
  private Long id;

  @Schema(description = "Source identifier of the resource", example = "SRC-56789")
  private String sourceId;

  @Schema(description = "Name of the resource", example = "Clinical Data Repository")
  private String name;

  @Schema(description = "Description of the resource", example = "A repository for clinical data.")
  private String description = "";

  @Schema(description = "Contact email for the resource", example = "support@resource.org")
  private String contactEmail;

  @Schema(description = "URI of the resource", example = "https://resource.org")
  private String uri;

  public ResourceResponseModel(Long id, String sourceId, String name) {
    this.id = id;
    this.sourceId = sourceId;
    this.name = name;
  }

  public ResourceResponseModel(
      Long id, String sourceId, String name, String description, String contactEmail, String uri) {
    this.id = id;
    this.sourceId = sourceId;
    this.name = name;
    this.description = description;
    this.contactEmail = contactEmail;
    this.uri = uri;
  }
}
