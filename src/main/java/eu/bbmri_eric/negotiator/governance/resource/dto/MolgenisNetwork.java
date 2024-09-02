package eu.bbmri_eric.negotiator.governance.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class MolgenisNetwork implements GenericNetwork {

  String id;

  String name;

  String contactEmail;

  String uri;
}
