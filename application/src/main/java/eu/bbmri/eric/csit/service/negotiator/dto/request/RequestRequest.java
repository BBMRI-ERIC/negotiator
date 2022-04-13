package eu.bbmri.eric.csit.service.negotiator.dto.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.model.Query;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class RequestRequest {
  @NotNull
  private String title;

  @NotNull
  private String description;

  @NotNull
  private String projectDescription;

  @NotNull
  private String ethicsVote;

  @Nullable
  private Boolean isTest;

  @NotNull
  private Set<Query> queries;

}
