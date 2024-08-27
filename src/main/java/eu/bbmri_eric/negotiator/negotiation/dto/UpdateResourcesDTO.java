package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UpdateResourcesDTO {
  @NotNull(message = "You must provide at least one resource to update.")
  private List<Long> resourceIds;
  private NegotiationResourceState state = NegotiationResourceState.SUBMITTED;

  public UpdateResourcesDTO(List<Long> resourceIds) {
    this.resourceIds = resourceIds;
  }

  public UpdateResourcesDTO(List<Long> resourceIds, NegotiationResourceState state) {
    this.resourceIds = resourceIds;
    this.state = state;
  }
}
