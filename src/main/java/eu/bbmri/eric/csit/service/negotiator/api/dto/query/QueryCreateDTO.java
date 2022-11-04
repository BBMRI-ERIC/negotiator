package eu.bbmri.eric.csit.service.negotiator.api.dto.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class QueryCreateDTO {

  @NotNull(message = "The url of the original query must be present")
  private String url;

  @NotNull(message = "A human readable description of the query must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<ResourceDTO> resources;
}
