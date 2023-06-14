package eu.bbmri.eric.csit.service.negotiator.dto.perun;

import eu.bbmri.eric.csit.service.negotiator.dto.ValidationGroups;
import javax.validation.constraints.NotNull;
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
