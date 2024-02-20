package eu.bbmri_eric.negotiator.dto.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.dto.OrganizationDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

/** Resource DTO containing the status of the Resource, to be used as part of the Negotiation */
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
