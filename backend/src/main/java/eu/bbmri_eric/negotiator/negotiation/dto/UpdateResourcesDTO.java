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
   * <p><b>Hazard, recorded rather than solved.</b> This is the one place in the codebase where a
   * Resource State name is a <em>default</em> rather than a comparison, and a default is the worse
   * kind of dependency: a request that names no State silently gets this one. It is wrong for any
   * Definition Family whose vocabulary lacks {@code SUBMITTED}, and unlike a comparison - which
   * simply stops matching - it writes a name the family does not have. Changing the value is a
   * behaviour change, so this slab leaves it exactly as it was; whoever ships the second Resource
   * Definition Family has to decide whether the default belongs here at all, or whether an absent
   * State should mean "leave it alone" or resolve off the Definition Version instead.
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
