package eu.bbmri.eric.csit.service.negotiator.api.dto.project;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ProjectDTO extends ProjectCreateDTO {

  @NotNull
  private String id;
}
