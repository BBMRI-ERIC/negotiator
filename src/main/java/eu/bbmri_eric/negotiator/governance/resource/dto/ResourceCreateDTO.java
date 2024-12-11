package eu.bbmri_eric.negotiator.governance.resource.dto;

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
public class ResourceCreateDTO {

  @NotNull private String name;
  private String description;
  @NotNull private String sourceId;
  @NotNull private Long organizationId;
  @NotNull private Long accessFormId;
  @NotNull private Long discoveryServiceId;
  private String contactEmail;
}
