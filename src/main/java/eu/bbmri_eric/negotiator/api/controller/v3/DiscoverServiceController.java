package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.ValidationGroups.Create;
import eu.bbmri_eric.negotiator.dto.ValidationGroups.Update;
import eu.bbmri_eric.negotiator.dto.discoveryservice.DiscoveryServiceCreateDTO;
import eu.bbmri_eric.negotiator.dto.discoveryservice.DiscoveryServiceDTO;
import eu.bbmri_eric.negotiator.service.DiscoveryServiceService;
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
public class DiscoverServiceController {

  @Autowired private DiscoveryServiceService discoveryServiceService;
  @Autowired private ModelMapper modelMapper;

  @GetMapping("/discovery-service")
  List<DiscoveryServiceDTO> list() {
    return discoveryServiceService.findAll();
  }

  @GetMapping("/discovery-service/{id}")
  DiscoveryServiceDTO retrieve(@PathVariable Long id) {
    return discoveryServiceService.findById(id);
  }

  @PostMapping(
      value = "/discovery-service",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DiscoveryServiceDTO add(@Validated(Create.class) @RequestBody DiscoveryServiceCreateDTO request) {
    return discoveryServiceService.create(request);
  }

  @PutMapping(
      value = "/discovery-service/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  DiscoveryServiceDTO update(
      @PathVariable Long id,
      @Validated(Update.class) @RequestBody DiscoveryServiceCreateDTO request) {
    return discoveryServiceService.update(id, request);
  }
}
