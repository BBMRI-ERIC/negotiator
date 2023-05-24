package eu.bbmri.eric.csit.service.negotiator.service.dto.perun;

import eu.bbmri.eric.csit.service.negotiator.service.dto.ValidationGroups;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerunMappingRequest {

  @NotNull(groups = ValidationGroups.Create.class)
  private String organization;

  @NotNull(groups = ValidationGroups.Create.class)
  private Integer id;

  @NotNull(groups = ValidationGroups.Create.class)
  private String displayName;

  @NotNull(groups = ValidationGroups.Create.class)
  private String status;

  @NotNull(groups = ValidationGroups.Create.class)
  private String mail;

  @NotNull(groups = ValidationGroups.Create.class)
  private String[] identities;
}
