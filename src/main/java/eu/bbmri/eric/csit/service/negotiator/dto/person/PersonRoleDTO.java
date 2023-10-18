package eu.bbmri.eric.csit.service.negotiator.dto.person;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PersonRoleDTO {

  @NotNull
  // TODO: is it necessary?
  private String id;

  @NotNull
  // TODO: is it necessary?
  private String authSubject;

  @NotNull private String name;

  @NotNull private String role;
}
