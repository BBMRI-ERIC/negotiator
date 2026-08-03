package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
  @NotEmpty(message = "You must provide at least one resource to update.")
  private List<Long> resourceIds;

  private NegotiationResourceState state = NegotiationResourceState.SUBMITTED;

  @Schema(
      description =
          "Display name of the help desk actor who triggered this update (e.g. the representative's name or email). "
              + "Only accepted from callers holding ROLE_HELPDESK_INTEGRATION. Stored for display purposes only.",
      example = "john.smith@mhh.de")
  private String helpdeskActor;

  public UpdateResourcesDTO(List<Long> resourceIds) {
    this.resourceIds = resourceIds;
  }

  public UpdateResourcesDTO(List<Long> resourceIds, NegotiationResourceState state) {
    this.resourceIds = resourceIds;
    this.state = state;
  }
}
