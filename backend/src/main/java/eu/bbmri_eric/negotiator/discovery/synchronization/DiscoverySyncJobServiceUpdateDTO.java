package eu.bbmri_eric.negotiator.discovery.synchronization;

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
public class DiscoverySyncJobServiceUpdateDTO {
  @NotNull private DiscoveryServiceSyncronizationJobStatus jobStatus;
}
