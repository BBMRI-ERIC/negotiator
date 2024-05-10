package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.service.MolgenisDirectorySyncService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
@SecurityRequirement(name = "security_auth")
public class MolgenisDirectorySyncController {

  @Autowired MolgenisDirectorySyncService molgenisDirectorySyncService;

  @PostMapping(value = "/sync_resources")
  @ResponseStatus(HttpStatus.CREATED)
  public String syncResourcesWithMolgenis() {
    log.info("MolgenisDirectorySyncController::attempt to sync directory resources...");
    molgenisDirectorySyncService.syncDirectoryResources();
    return ("Sync job properly instantiated");
  }
}
