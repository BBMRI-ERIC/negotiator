package eu.bbmri.eric.csit.service.negotiator.api.dto.perun;

import eu.bbmri.eric.csit.service.negotiator.api.dto.ValidationGroups.Create;
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

  @NotNull(groups = Create.class)
  private String organization;

  @NotNull(groups = Create.class)
  private Integer id;

  @NotNull(groups = Create.class)
  private String displayName;

  @NotNull(groups = Create.class)
  private String status;

  @NotNull(groups = Create.class)
  private String mail;

  @NotNull(groups = Create.class)
  private String[] identities;
}
