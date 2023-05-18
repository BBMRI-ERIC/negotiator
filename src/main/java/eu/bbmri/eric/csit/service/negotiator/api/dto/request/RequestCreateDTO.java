package eu.bbmri.eric.csit.service.negotiator.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class RequestCreateDTO {

  @NotNull(message = "The url of the original request must be present")
  private String url;

  @NotNull(message = "A human readable description of the request must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<ResourceDTO> resources;
}
