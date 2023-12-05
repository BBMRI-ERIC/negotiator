package eu.bbmri.eric.csit.service.negotiator.dto.person;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResourceModel {
  String id;
  String externalId;
  String name;
}
