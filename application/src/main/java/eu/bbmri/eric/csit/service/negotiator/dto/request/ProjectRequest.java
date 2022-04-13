package eu.bbmri.eric.csit.service.negotiator.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.OptBoolean;
import java.util.Date;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ProjectRequest {
  @NotNull private String title;

  @NotNull private String description;

  @NotNull private String ethicsVote;

  @NotNull
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd", lenient = OptBoolean.FALSE)
  private Date expectedEndDate;

  @NotNull private Boolean expectedDataGeneration;

  private Boolean isTestProject = false;
}
