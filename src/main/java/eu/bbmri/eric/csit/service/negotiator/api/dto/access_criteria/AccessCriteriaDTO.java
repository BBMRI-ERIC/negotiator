package eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.istack.NotNull;
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
public class AccessCriteriaDTO {

  @NotNull
  private String name;

  @NotNull
  private String label;

  @NotNull
  private String description;

  @NotNull
  private String type;

  @NotNull
  private Boolean required;

}
