package eu.bbmri.eric.csit.service.negotiator.service.dto.datasource;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class DataSourceDTO {

  private Long id;

  private String description;

  private String name;

  private String url;
}
