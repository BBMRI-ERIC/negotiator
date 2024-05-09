package eu.bbmri_eric.negotiator.api.controller.v3.tasks;

import eu.bbmri_eric.negotiator.service.MolgenisDirectorySyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledDirectorySyncTask {

  @Autowired private MolgenisDirectorySyncService molgenisDirectorySyncService;

  @Scheduled(cron = "0 * * * * *") // Cron expression for running every minute
  public void syncMolgenisResources() {
    molgenisDirectorySyncService.syncDirectoryResources();
  }
}
