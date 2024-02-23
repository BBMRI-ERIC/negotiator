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

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ResourceDTO {

  @NotNull private String id;

  @NotNull private String externalId;

  @Nullable private String name;

  @NotNull private OrganizationDTO organization;
}
