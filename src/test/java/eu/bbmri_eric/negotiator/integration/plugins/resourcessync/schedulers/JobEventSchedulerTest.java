package eu.bbmri_eric.negotiator.integration.plugins.resourcessync.schedulers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.discovery_service.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery_service.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery_service.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.discovery_service.DiscoverySynchronizationJobService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class JobEventSchedulerTest {

  @Autowired private DiscoveryServiceRepository testServiceRepository;

  @Autowired private DiscoverySynchronizationJobService testDiscoverySyncJobService;

  @Autowired private DiscoveryServiceSynchronizationJobRepository testJobRepository;

  @Autowired private ScheduledTaskHolder scheduledTaskHolder;

  private DiscoveryService testDiscoveryservice;

  @Value("${synchronization.frequency}")
  private String frequency;

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
    Thread.sleep(1000);
    assertEquals(1, testJobRepository.findAll().size());
  }

  @Test
  void testJobSchedulingByChron() {
    Set<ScheduledTask> scheduledTasks = scheduledTaskHolder.getScheduledTasks();
    scheduledTasks.forEach(
        scheduledTask -> scheduledTask.getTask().getRunnable().getClass().getDeclaredMethods());
    long count =
        scheduledTasks.stream()
            .filter(scheduledTask -> scheduledTask.getTask() instanceof CronTask)
            .map(scheduledTask -> (CronTask) scheduledTask.getTask())
            .filter(cronTask -> cronTask.getExpression().equals(frequency))
            .count();
    assertEquals(count, 1L);
  }
}
