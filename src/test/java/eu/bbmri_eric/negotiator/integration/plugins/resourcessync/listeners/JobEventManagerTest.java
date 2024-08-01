package eu.bbmri_eric.negotiator.integration.plugins.resourcessync.listeners;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.plugins.resourcesync.listeners.JobEventManager;
import eu.bbmri_eric.negotiator.plugins.resourcesync.publishers.JobEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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
