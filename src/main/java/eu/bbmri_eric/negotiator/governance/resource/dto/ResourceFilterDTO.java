package eu.bbmri_eric.negotiator.governance.resource.dto;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Filters for the Resource controller. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ResourceFilterDTO implements FilterDTO {
  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  private int page = 0;

  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  private int size = 50;

  private String name;
  private String sourceId;

}
