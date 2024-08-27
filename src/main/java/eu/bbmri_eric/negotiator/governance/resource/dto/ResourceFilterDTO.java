package eu.bbmri_eric.negotiator.governance.resource.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResourceFilterDTO {
  private String name;
  private String sourceId;

  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  private int page = 0;

  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  private int size = 50;
}
