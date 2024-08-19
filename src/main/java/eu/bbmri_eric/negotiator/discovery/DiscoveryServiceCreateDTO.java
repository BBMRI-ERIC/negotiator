package eu.bbmri_eric.negotiator.discovery;

import eu.bbmri_eric.negotiator.common.ValidationGroups;
import jakarta.validation.constraints.NotNull;
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
public class DiscoveryServiceCreateDTO {

  @NotNull(groups = ValidationGroups.Create.class)
  private String name;

  @NotNull(groups = ValidationGroups.Create.class)
  private String url;
}
