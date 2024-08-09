package eu.bbmri_eric.negotiator.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class SubmittedInformationDTO {
  @NotNull private Long id;
  @NotNull private Long resourceId;
  private Long requirementId;
  @NotNull private JsonNode payload;
}
