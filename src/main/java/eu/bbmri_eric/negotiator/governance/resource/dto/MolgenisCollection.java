package eu.bbmri_eric.negotiator.governance.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MolgenisCollection implements GenericResource {
  private String id;
  private String name;
  private String description;
  private MolgenisBiobank biobank;

  @Override
  public GenericOrganization getOrganization() {
    return null;
  }
}
