package eu.bbmri_eric.negotiator.discovery;

import eu.bbmri_eric.negotiator.common.ValidationGroups.Create;
import eu.bbmri_eric.negotiator.common.ValidationGroups.Update;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Validated
@Tag(name = "Discovery services", description = "Manage connected discovery services")
@SecurityRequirement(name = "security_auth")
public class DiscoverServiceController {

  @Autowired private DiscoveryServiceService discoveryServiceService;
  @Autowired private ModelMapper modelMapper;

  @Autowired private DiscoverySynchronizationJobService discoverySynchronizationJobService;

  @GetMapping("/discovery-services")
  List<DiscoveryServiceDTO> list() {
    return discoveryServiceService.findAll();
  }

  @GetMapping("/discovery-services/{id}")
  DiscoveryServiceDTO retrieve(@PathVariable Long id) {
    return discoveryServiceService.findById(id);
  }

  @PostMapping(
      value = "/discovery-services",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DiscoveryServiceDTO add(@Validated(Create.class) @RequestBody DiscoveryServiceCreateDTO request) {
    return discoveryServiceService.create(request);
  }

  @PutMapping(
      value = "/discovery-services/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  DiscoveryServiceDTO update(
      @PathVariable Long id,
      @Validated(Update.class) @RequestBody DiscoveryServiceCreateDTO request) {
    return discoveryServiceService.update(id, request);
  }

  @PostMapping(
      value = "/discovery-services/{id}/sync-job",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DiscoverySyncJobServiceDTO add(@PathVariable Long id) {
    return discoverySynchronizationJobService.createSyncJob(id);
  }
}
