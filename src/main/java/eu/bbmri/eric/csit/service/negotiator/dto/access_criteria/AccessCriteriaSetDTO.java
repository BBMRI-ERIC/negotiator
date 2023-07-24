package eu.bbmri.eric.csit.service.negotiator.dto.access_criteria;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class AccessCriteriaSetDTO {

  String name;

  List<AccessCriteriaSectionDTO> sections = new ArrayList<>();
}
