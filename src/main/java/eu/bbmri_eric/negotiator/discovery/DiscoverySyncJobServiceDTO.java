package eu.bbmri_eric.negotiator.discovery;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
public class DiscoverySyncJobServiceDTO {

  @NotNull private String id;
  @NotNull private String discoveryServiceName;
  @NotNull private LocalDateTime creationDate;
  @NotNull private LocalDateTime modifiedDate;
}
