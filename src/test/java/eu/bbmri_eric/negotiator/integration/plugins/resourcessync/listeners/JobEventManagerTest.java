package eu.bbmri_eric.negotiator.integration.plugins.resourcessync.listeners;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceClient;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.discovery.synchronization.DiscoveryServiceSyncronizationJobStatus;
import eu.bbmri_eric.negotiator.discovery.synchronization.JobEventManager;
import eu.bbmri_eric.negotiator.discovery.synchronization.JobEventPublisher;
import eu.bbmri_eric.negotiator.util.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@IntegrationTest(loadTestData = true)
public class JobEventManagerTest {

  @Autowired private JobEventPublisher testJobEventPublisher;

  @Autowired private JobEventManager testJobEventmanager;

  @Autowired private DiscoveryServiceSynchronizationJobRepository testJobRepository;

  @Autowired private DiscoveryServiceRepository testServiceRepository;

  @MockBean private WebClient webClient;

  @MockBean private DiscoveryServiceClient discoveryService;

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
    Thread.sleep(500);
    DiscoveryServiceSynchronizationJob retrievedJob =
        testJobRepository.findDetailedById(job.getId()).get();
    assertEquals(retrievedJob.getStatus(), DiscoveryServiceSyncronizationJobStatus.COMPLETED);
  }

  @Test
  void testWhenPublishingDiscoveryServiceSyncEventWithServiceUnrecheable()
      throws InterruptedException {
    doThrow(new WebClientResponseException(500, "Not Recheable", null, null, null))
        .when(discoveryService)
        .syncAllOrganizations();
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
    Thread.sleep(500);
    assertThrows(
        WebClientResponseException.class,
        () -> {
          discoveryService.syncAllOrganizations();
        });
    DiscoveryServiceSynchronizationJob retrievedJob =
        testJobRepository.findDetailedById(job.getId()).get();
    assertEquals(
        retrievedJob.getStatus(), DiscoveryServiceSyncronizationJobStatus.COMPLETED_WITH_ERRORS);
  }

  @Test
  void testWhenPublishingDiscoveryServiceSyncEventWithServiceThrowingStorageError()
      throws InterruptedException {
    doThrow(new EntityNotStorableException()).when(discoveryService).syncAllOrganizations();
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
    Thread.sleep(500);
    assertThrows(
        EntityNotStorableException.class,
        () -> {
          discoveryService.syncAllOrganizations();
        });
    DiscoveryServiceSynchronizationJob retrievedJob =
        testJobRepository.findDetailedById(job.getId()).get();
    assertEquals(
        retrievedJob.getStatus(), DiscoveryServiceSyncronizationJobStatus.COMPLETED_WITH_ERRORS);
  }

  @Test
  void testWhenPublishingDiscoveryServiceSyncEventAllExecutionOK() throws InterruptedException {
    doNothing().when(discoveryService).syncAllOrganizations();
    doNothing().when(discoveryService).syncAllResources();
    doNothing().when(discoveryService).syncAllNetworks();
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
    Thread.sleep(500);
    DiscoveryServiceSynchronizationJob retrievedJob =
        testJobRepository.findDetailedById(job.getId()).get();
    assertEquals(retrievedJob.getStatus(), DiscoveryServiceSyncronizationJobStatus.COMPLETED);
  }
}
