package eu.bbmri_eric.negotiator.integration.plugins.resourcessync.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.discovery.synchronization.JobEventManager;
import eu.bbmri_eric.negotiator.discovery.synchronization.JobEventPublisher;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest(loadTestData = true)
public class JobEventManagerTest {

  @Autowired private JobEventPublisher testJobEventPublisher;

  @Autowired private JobEventManager testJobEventmanager;

  @Autowired private DiscoveryServiceSynchronizationJobRepository testJobRepository;

  @Autowired private DiscoveryServiceRepository testServiceRepository;

  @Test
  void testWhenPublishingDiscoveryServiceSyncEvent() throws InterruptedException {
    DiscoveryService service =
        DiscoveryService.builder()
            .id(Long.valueOf("1"))
            .name("testservice")
            .url("http://testservice.net")
            .build();
    testServiceRepository.save(service);
    DiscoveryServiceSynchronizationJob job =
        DiscoveryServiceSynchronizationJob.builder()
            .status(DiscoveryServiceSyncronizationJobStatus.SUBMITTED)
            .service(service)
            .build();
    testJobRepository.save(job);
    testJobEventPublisher.publishDiscoveryServiceSynchronizationEvent(
        job.getId(), Long.valueOf("1"));
    Thread.sleep(5000);
    DiscoveryServiceSynchronizationJob retrievedJob =
        testJobRepository.findDetailedById(job.getId()).get();
    assertEquals(retrievedJob.getStatus(), DiscoveryServiceSyncronizationJobStatus.IN_PROGRESS);
  }
}
