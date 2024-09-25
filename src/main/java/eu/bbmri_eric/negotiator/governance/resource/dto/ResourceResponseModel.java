package eu.bbmri_eric.negotiator.governance.resource.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

/** A DTO for an abstract resource in the Negotiator. */
@Getter
@Setter
@Relation(collectionRelation = "resources", itemRelation = "resource")
@NoArgsConstructor
public class ResourceResponseModel {
  Long id;
  String sourceId;
  String name;
  String description = "";

  public ResourceResponseModel(Long id, String sourceId, String name) {
    this.id = id;
    this.sourceId = sourceId;
    this.name = name;
  }

  public ResourceResponseModel(Long id, String sourceId, String name, String description) {
    this.id = id;
    this.sourceId = sourceId;
    this.name = name;
    this.description = description;
  }
}
