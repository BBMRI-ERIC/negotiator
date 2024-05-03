package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceDTO;
import eu.bbmri_eric.negotiator.service.NetworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
@RequestMapping("/v3")
@Tag(name = "Networks", description = "Retrieve connected networks")
public class NetworkController {

  private final NetworkService networkService;

  public NetworkController(NetworkService networkService) {
      this.networkService = networkService;
  }

  @GetMapping("/networks")
  @Operation(summary = "List all networks")
  public List<NetworkDTO> list() {
    return networkService.findAllNetworks();
  }

  @GetMapping("/networks/{id}")
  @Operation(summary = "Get network by id")
  public NetworkDTO findById(@PathVariable("id") Long id) {
    return networkService.findNetworkById(id);
  }

  @GetMapping("/networks/{id}/resources")
  @Operation(summary = "List all resources of a network")
  public List<ResourceDTO> getResources(@PathVariable("id") Long id) {
      return networkService.getResources(id);
  }



}
