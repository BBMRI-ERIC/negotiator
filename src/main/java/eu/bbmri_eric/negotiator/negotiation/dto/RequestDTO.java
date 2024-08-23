package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class RequestDTO {

  @NotNull private String id;

  @NotNull(message = "The url of the original request must be present")
  private String url;

  @NotNull(message = "A human readable description of the request must be present")
  private String humanReadable;

  @NotNull
  @NotEmpty(message = "At least one resource must be present")
  private Set<ResourceDTO> resources;

  @NotNull private String redirectUrl;

  private String negotiationId;
}
