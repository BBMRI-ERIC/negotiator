package eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
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
public class NegotiationDTO {

  @NotNull
  private String id;

  @NotNull
  private String title;

  @NotNull
  private String description;

  private Boolean isTest = false;

  private ProjectDTO project;

  private Set<RequestDTO> queries;

  private Set<PersonRoleDTO> persons;
}
