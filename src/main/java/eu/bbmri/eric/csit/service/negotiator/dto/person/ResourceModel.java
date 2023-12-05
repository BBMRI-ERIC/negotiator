package eu.bbmri.eric.csit.service.negotiator.dto.person;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourceModel extends RepresentationModel<ResourceModel> {
  String type = "resources";
  String id;
  String externalId;
  String name;
}
