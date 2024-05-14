package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.NetworkDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.dto.person.UserResponseModel;
import eu.bbmri_eric.negotiator.mappers.NegotiationModelAssembler;
import eu.bbmri_eric.negotiator.mappers.NetworkModelAssembler;
import eu.bbmri_eric.negotiator.mappers.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.service.NegotiationService;
import eu.bbmri_eric.negotiator.service.NetworkService;
import eu.bbmri_eric.negotiator.service.PersonService;
import eu.bbmri_eric.negotiator.service.ResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Tag(name = "Networks", description = "Retrieve connected networks")
public class NetworkController {

  private final NetworkService networkService;
  private final PersonService personService;
  private final ResourceService resourceService;
  private final NegotiationService negotiationService;
  private final NetworkModelAssembler networkModelAssembler;
  private final ResourceModelAssembler resourceModelAssembler;
  private final NegotiationModelAssembler negotiationModelAssembler =
      new NegotiationModelAssembler();

  public NetworkController(
      NetworkService networkService,
      NetworkModelAssembler networkModelAssembler,
      ResourceService resourceService,
      ResourceModelAssembler resourceModelAssembler,
      PersonService personService,
      NegotiationService negotiationService) {
    this.networkService = networkService;
    this.networkModelAssembler = networkModelAssembler;
    this.resourceService = resourceService;
    this.resourceModelAssembler = resourceModelAssembler;
    this.personService = personService;
    this.negotiationService = negotiationService;
  }

  @GetMapping("/networks")
  @Operation(summary = "List all networks")
  public PagedModel<EntityModel<NetworkDTO>> list(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return networkModelAssembler.toPagedModel(
        (Page<NetworkDTO>) networkService.findAllNetworks(PageRequest.of(page, size)));
  }

  @GetMapping("/networks/{id}")
  @Operation(summary = "Get network by id")
  public NetworkDTO findById(@PathVariable("id") Long id) {
    return networkService.findNetworkById(id);
  }

  @GetMapping("/networks/{id}/resources")
  @Operation(summary = "List all resources of a network")
  public PagedModel<EntityModel<ResourceResponseModel>> getResources(
      @PathVariable("id") Long id,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return resourceModelAssembler.toPagedModel(
        (Page<ResourceResponseModel>)
            resourceService.findAllForNetwork(PageRequest.of(page, size), id));
  }

  @GetMapping("/networks/{id}/managers")
  @Operation(summary = "List all managers of a network")
  public List<UserResponseModel> getManagers(@PathVariable("id") Long id) {
    return personService.findAllForNetwork(id);
  }

  @GetMapping("/networks/{id}/negotiations")
  @Operation(summary = "List all negotiations associated a network")
  public PagedModel<EntityModel<NegotiationDTO>> getNegotiations(
      @PathVariable("id") Long id,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return negotiationModelAssembler.toPagedModel(
        (Page<NegotiationDTO>)
            negotiationService.findAllForNetwork(PageRequest.of(page, size), id));
  }
}
