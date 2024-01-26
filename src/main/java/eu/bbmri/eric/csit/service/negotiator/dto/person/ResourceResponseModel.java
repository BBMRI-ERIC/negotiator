package eu.bbmri.eric.csit.service.negotiator.dto.person;

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
