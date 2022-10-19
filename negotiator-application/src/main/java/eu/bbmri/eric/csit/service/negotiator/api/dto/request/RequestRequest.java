package eu.bbmri.eric.csit.service.negotiator.api.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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
public class RequestRequest {

  @NotNull private String title;

  @NotNull private String description;

  private Boolean isTest = false;

  @Valid private ProjectRequest project;

  @Valid @NotEmpty private Set<Long> queries;
}
