package eu.bbmri_eric.negotiator.governance.resource.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.lang.Nullable;

/** A DTO depicting an abstract resource coming in a request from a Discovery Service */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Deprecated
public class ResourceDTO {

  @NotNull private String id;

  @Nullable private String name;

  @NotNull private OrganizationDTO organization;
}
