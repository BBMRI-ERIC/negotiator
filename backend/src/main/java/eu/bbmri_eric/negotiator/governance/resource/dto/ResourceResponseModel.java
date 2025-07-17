package eu.bbmri_eric.negotiator.governance.resource.dto;

import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;

/** A DTO for an abstract resource in the Negotiator. */
@Getter
@Setter
@Relation(collectionRelation = "resources", itemRelation = "resource")
@NoArgsConstructor
@Schema(description = "A DTO representing an abstract resource in the Negotiator.")
@ToString
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

  @Schema(description = "If the Resource is still active or not", example = "false")
  private boolean withdrawn;

  @Schema(description = "URI of the resource", example = "https://resource.org")
  private String uri;

  private OrganizationDTO organization;

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

  public ResourceResponseModel(
      Long id,
      String sourceId,
      String name,
      String description,
      String contactEmail,
      boolean withdrawn,
      String uri,
      OrganizationDTO organization) {
    this.id = id;
    this.sourceId = sourceId;
    this.name = name;
    this.description = description;
    this.contactEmail = contactEmail;
    this.withdrawn = withdrawn;
    this.uri = uri;
    this.organization = organization;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    ResourceResponseModel that = (ResourceResponseModel) o;
    return withdrawn == that.withdrawn
        && Objects.equals(id, that.id)
        && Objects.equals(sourceId, that.sourceId)
        && Objects.equals(name, that.name)
        && Objects.equals(description, that.description)
        && Objects.equals(contactEmail, that.contactEmail)
        && Objects.equals(uri, that.uri)
        && Objects.equals(organization, that.organization);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, sourceId, name, description, contactEmail, withdrawn, uri, organization);
  }
}
