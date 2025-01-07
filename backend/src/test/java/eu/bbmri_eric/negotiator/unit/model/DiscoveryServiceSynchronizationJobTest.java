package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSyncronizationJobStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class DiscoveryServiceSynchronizationJobTest {

  DiscoveryService service =
      DiscoveryService.builder()
          .id(Long.parseLong("1"))
          .name("service")
          .url("https://service.info")
          .build();

  @Test
  void testBuildCreateSyncJobOK() {
    DiscoveryServiceSynchronizationJob job =
        DiscoveryServiceSynchronizationJob.builder()
            .service(service)
            .status(DiscoveryServiceSyncronizationJobStatus.SUBMITTED)
            .creationDate(LocalDateTime.now())
            .modifiedDate(LocalDateTime.now())
            .build();
    assertInstanceOf(DiscoveryServiceSynchronizationJob.class, job);
  }
}
