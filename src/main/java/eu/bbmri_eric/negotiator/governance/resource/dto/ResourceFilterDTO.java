package eu.bbmri_eric.negotiator.governance.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResourceFilterDTO {
  private String name;
  private int page = 0;
  private int size = 50;
}
