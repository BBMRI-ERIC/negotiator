package eu.bbmri.eric.csit.service.negotiator.dto.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.dto.OrganizationDTO;
import javax.validation.constraints.NotNull;
import lombok.*;
import org.springframework.lang.Nullable;

/**
 * Resource DTO containing the status of the Resource, to be used as part of the Negotiation
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ResourceWithStatusDTO {

  @NotNull private String id;

  @Nullable private String name;

  @Nullable private OrganizationDTO organization;

  private String status;
}
