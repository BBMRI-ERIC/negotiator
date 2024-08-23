package eu.bbmri_eric.negotiator.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Relation(collectionRelation = "resources", itemRelation = "resource")
public class ResourceResponseModel {
  String id;
  String externalId;
  String name;
}
