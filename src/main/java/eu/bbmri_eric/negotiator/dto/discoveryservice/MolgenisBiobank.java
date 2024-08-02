package eu.bbmri_eric.negotiator.dto.discoveryservice;

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
