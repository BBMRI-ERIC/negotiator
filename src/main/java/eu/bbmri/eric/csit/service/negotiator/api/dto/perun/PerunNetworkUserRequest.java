package eu.bbmri.eric.csit.service.negotiator.api.dto.perun;

import eu.bbmri.eric.csit.service.negotiator.api.dto.ValidationGroups.Create;
import java.util.HashMap;
import java.util.List;
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
public class PerunNetworkUserRequest {

  @NotNull(groups = Create.class)
  private String name;

  @NotNull(groups = Create.class)
  private String directory;

  @NotNull(groups = Create.class)
  private String id;

  @NotNull(groups = Create.class)
  private List<HashMap> members;
}
