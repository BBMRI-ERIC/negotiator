package eu.bbmri_eric.negotiator.governance.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MolgenisBiobank implements GenericOrganization {
  String id;
  String name;
  String _href;
}
