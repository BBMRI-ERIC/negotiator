package eu.bbmri_eric.negotiator.dto.discoveryservice;

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
public class DiscoveryServiceDTO {

  private Long id;

  private String description;

  private String name;

  private String url;
}
