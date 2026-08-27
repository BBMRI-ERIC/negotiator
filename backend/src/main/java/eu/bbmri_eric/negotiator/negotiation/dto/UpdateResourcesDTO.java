package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import eu.bbmri_eric.negotiator.lifecycle.WellKnownResourceStates;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceStateNameDeserializer;
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

  /**
   * Name of the State to put the listed Resources into.
   *
   * <p>The default is a Well-known State name. A request that names no State gets {@code
   * SUBMITTED}, which is wrong for any Resource Definition Family that has no such State: the
   * update then writes a name the family does not contain.
   */
  @Schema(
      description = "Name of the state to put the listed resources into",
      example = "RESOURCE_AVAILABLE")
  @JsonDeserialize(using = NegotiationResourceStateNameDeserializer.class)
  private String state = WellKnownResourceStates.SUBMITTED;

  public UpdateResourcesDTO(List<Long> resourceIds) {
    this.resourceIds = resourceIds;
  }

  public UpdateResourcesDTO(List<Long> resourceIds, String state) {
    this.resourceIds = resourceIds;
    this.state = state;
  }
}
