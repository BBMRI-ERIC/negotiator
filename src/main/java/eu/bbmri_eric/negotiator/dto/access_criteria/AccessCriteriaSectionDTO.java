package eu.bbmri_eric.negotiator.dto.access_criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sun.istack.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class AccessCriteriaSectionDTO {

  @NotNull private String name;

  @NotNull private String label;

  @NotNull private String description;

  @NotNull private List<AccessCriteriaDTO> accessCriteria = new ArrayList<>();
}
