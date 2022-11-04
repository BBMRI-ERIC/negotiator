package eu.bbmri.eric.csit.service.negotiator.api.dto.project;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ProjectCreateDTO {
  @NotNull private String title;

  @NotNull
  @Length(max = 512)
  private String description;

  @Length(max = 512)
  @NotNull private String ethicsVote;

  @NotNull
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate expectedEndDate;

  @NotNull private Boolean expectedDataGeneration;

  private Boolean isTestProject = false;
}
