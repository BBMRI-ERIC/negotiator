package eu.bbmri_eric.negotiator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MolgenisCollection {
  private String id;
  private String name;
  private MolgenisBiobank biobank;
}
