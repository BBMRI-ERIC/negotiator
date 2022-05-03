package eu.bbmri.eric.csit.service.negotiator.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.OptBoolean;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class ProjectRequest {
  @NotNull private String title;

  @NotNull private String description;

  @NotNull private String ethicsVote;

  @NotNull
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate expectedEndDate;

  @NotNull private Boolean expectedDataGeneration;

  private Boolean isTestProject = false;
}
