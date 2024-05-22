package eu.bbmri_eric.negotiator.dto.syncjobservice;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiscoverySyncJobServiceCreateDTO {

  @NotNull private String discoveryServiceName;
}
