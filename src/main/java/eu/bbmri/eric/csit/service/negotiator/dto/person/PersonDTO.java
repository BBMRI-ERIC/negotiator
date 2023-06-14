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
public class PersonDTO {

  @NotNull
  private String name;

  @NotNull
  private String organization;

  @NotNull
  private String role;

}