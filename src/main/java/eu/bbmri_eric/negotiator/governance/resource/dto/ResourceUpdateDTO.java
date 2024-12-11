package eu.bbmri_eric.negotiator.governance.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUpdateDTO {

  private String name;
  private String description;
  private String contactEmail;
}
