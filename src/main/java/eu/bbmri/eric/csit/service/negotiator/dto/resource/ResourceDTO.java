package eu.bbmri.eric.csit.service.negotiator.dto.resource;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.dto.OrganizationDTO;
import javax.validation.constraints.NotNull;
import lombok.*;
import org.springframework.lang.Nullable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ResourceDTO {

  @NotNull private String id;

  @Nullable private String name;

  @NotNull private OrganizationDTO organization;
}
