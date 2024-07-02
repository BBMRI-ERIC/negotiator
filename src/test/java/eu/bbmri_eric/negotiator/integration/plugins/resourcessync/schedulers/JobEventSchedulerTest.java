package eu.bbmri_eric.negotiator.integration.plugins.resourcessync.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.service.DiscoverySynchronizationJobService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"synchronization.frequency=0 * * * * *"})
public class JobEventSchedulerTest {

  @Autowired private DiscoveryServiceRepository testServiceRepository;

  @Autowired private DiscoverySynchronizationJobService testDiscoverySyncJobService;

  @Autowired private DiscoveryServiceSynchronizationJobRepository testJobRepository;

  private DiscoveryService testDiscoveryservice;

  @BeforeEach
  void initializeDiscoveryService() {
    testJobRepository.deleteAll();
    testDiscoveryservice =
        DiscoveryService.builder()
            .id(Long.valueOf("1"))
            .name("testservice")
            .url("http://testservice.net")
            .build();
    testServiceRepository.save(testDiscoveryservice);
  }

  @Test
  void testJobTriggeredByService() throws InterruptedException {
    testDiscoverySyncJobService.createSyncJob(testDiscoveryservice.getId());
    Thread.sleep(5000);
    assertEquals(1, testJobRepository.findAll().size());
  }

  @Test
  void testJobTriggeredByChron() throws InterruptedException {

    Thread.sleep(65000);
    assertEquals(1, testJobRepository.findAll().size());
  }
}
