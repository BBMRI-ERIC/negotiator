package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.syncjobservice.DiscoverySyncJobServiceCreateDTO;
import eu.bbmri_eric.negotiator.dto.syncjobservice.DiscoverySyncJobServiceDTO;
import eu.bbmri_eric.negotiator.service.DiscoverySynchronizationJobService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
@SecurityRequirement(name = "security_auth")
@Tag(
    name = "Discovery service synchronization",
    description = "Submit and manage discovery service synchronization jobs")
public class DiscoverySyncronizationJobController {

  @Autowired private DiscoverySynchronizationJobService discoverySynchronizationJobService;

  @PostMapping(
      value = "/discoveryServiceSyncJob",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DiscoverySyncJobServiceDTO add(@Valid @RequestBody DiscoverySyncJobServiceCreateDTO request) {
    return discoverySynchronizationJobService.createSyncJob(request);
  }
}
