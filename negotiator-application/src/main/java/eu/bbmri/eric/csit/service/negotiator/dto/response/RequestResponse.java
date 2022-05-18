package eu.bbmri.eric.csit.service.negotiator.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
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

  private Set<QueryResponse> queries;

  private Set<PersonRequestRoleDTO> persons;
}
