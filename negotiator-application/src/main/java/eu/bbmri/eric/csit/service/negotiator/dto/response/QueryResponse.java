package eu.bbmri.eric.csit.service.negotiator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import java.util.Set;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class QueryResponse {

  @NotNull private Long id;

  @NotNull(message = "The url of the original query must be present")
  private String url;

  @NotNull(message = "A human readable description of the query must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<ResourceDTO> resources;

  @NotNull private String queryToken;

  @NotNull private String redirectUrl;
}
