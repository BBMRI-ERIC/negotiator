package eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.istack.NotNull;
import java.util.ArrayList;
import java.util.List;
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
public class AccessCriteriaSectionDTO {
  @NotNull
  private String title;

  @NotNull
  private String description;

  @NotNull
  List<AccessCriteriaDTO> accessCriteria = new ArrayList<>();

}
