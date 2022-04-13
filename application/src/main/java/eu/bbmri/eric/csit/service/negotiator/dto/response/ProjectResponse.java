package eu.bbmri.eric.csit.service.negotiator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
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
public class ProjectResponse extends ProjectRequest {

  @NotNull private Long id;
}
