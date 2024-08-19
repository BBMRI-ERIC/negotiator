package eu.bbmri_eric.negotiator.governance.resource.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.governance.OrganizationDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.lang.Nullable;

/** Resource DTO containing the status of the Resource, to be used as part of the Negotiation */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "resources", itemRelation = "resource")
public class ResourceWithStatusDTO {

  @NotNull private Long id;
  @NotNull private String sourceId;
  @Nullable private String name;
  @JsonIgnore private String negotiationId;

  @Nullable private OrganizationDTO organization;

  private NegotiationResourceState currentState;
}
