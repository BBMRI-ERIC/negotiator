package eu.bbmri_eric.negotiator.governance.organization;

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
public class OrganizationCreateDTO {

  @NotNull private String name;
  private String description;
  @NotNull private String externalId;
  private String contactEmail;
  private Boolean withdrawn;
}
