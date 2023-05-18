package eu.bbmri.eric.csit.service.negotiator.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ResourceDTO {

  @NotNull
  private String id;

  @Nullable
  private String name;

  @NotNull
  private String type;

  @NotNull
  private Set<ResourceDTO> children;
}
