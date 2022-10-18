package eu.bbmri.eric.csit.service.negotiator.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.query.QueryDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRequestRoleDTO;
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
public class RequestDTO {
  @NotNull private String id;

  @NotNull private String title;

  @NotNull private String description;

  @NotNull private String token;

  private Boolean isTest = false;

  private ProjectDTO project;

  private Set<QueryDTO> queries;

  private Set<PersonRequestRoleDTO> persons;
}
