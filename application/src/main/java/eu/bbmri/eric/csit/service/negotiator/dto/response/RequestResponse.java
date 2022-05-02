package eu.bbmri.eric.csit.service.negotiator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import java.util.List;
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
public class RequestResponse {
  @NotNull private Long id;

  @NotNull private String title;

  @NotNull private String description;

  @NotNull private String token;

  private Boolean isTest = false;

  private ProjectResponse project;

  private List<QueryResponse> queries;
}
