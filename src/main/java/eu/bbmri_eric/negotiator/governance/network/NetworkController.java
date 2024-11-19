package eu.bbmri_eric.negotiator.governance.network;

import eu.bbmri_eric.negotiator.common.ValidationGroups;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelAssembler;
import eu.bbmri_eric.negotiator.governance.resource.ResourceService;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationFilterDTO;
import eu.bbmri_eric.negotiator.negotiation.mappers.NegotiationModelAssembler;
import eu.bbmri_eric.negotiator.user.PersonService;
import eu.bbmri_eric.negotiator.user.UserModelAssembler;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Tag(name = "Networks", description = "Manage networks of resources and organizations")
public class NetworkController {

  private final NetworkService networkService;
  private final PersonService personService;
  private final ResourceService resourceService;
  private final NegotiationService negotiationService;
  private final NetworkModelAssembler networkModelAssembler;
  private final ResourceModelAssembler resourceModelAssembler;
  private final NegotiationModelAssembler negotiationModelAssembler;
  private final UserModelAssembler userModelAssembler;
  private final NetworkRepository networkRepository;
  private final NetworkStatisticsService networkStatisticsService;

  public NetworkController(
      NetworkService networkService,
      NetworkModelAssembler networkModelAssembler,
      ResourceService resourceService,
      ResourceModelAssembler resourceModelAssembler,
      PersonService personService,
      NegotiationService negotiationService,
      NegotiationModelAssembler negotiationModelAssembler,
      UserModelAssembler userModelAssembler,
      NetworkRepository networkRepository,
      NetworkStatisticsService networkStatisticsService) {
    this.networkService = networkService;
    this.networkModelAssembler = networkModelAssembler;
    this.resourceService = resourceService;
    this.resourceModelAssembler = resourceModelAssembler;
    this.personService = personService;
    this.negotiationService = negotiationService;
    this.negotiationModelAssembler = negotiationModelAssembler;
    this.userModelAssembler = userModelAssembler;
    this.networkRepository = networkRepository;
    this.networkStatisticsService = networkStatisticsService;
  }

  @GetMapping("/networks")
  @Operation(summary = "List all networks")
  public PagedModel<EntityModel<NetworkDTO>> list(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return networkModelAssembler.toPagedModel(
        (Page<NetworkDTO>) networkService.findAllNetworks(PageRequest.of(page, size)));
  }

  @PostMapping(
      value = "/networks",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Create a batch of networks")
  @ResponseStatus(HttpStatus.CREATED)
  public CollectionModel<EntityModel<NetworkDTO>> create(
      @Validated(ValidationGroups.Create.class) @RequestBody List<NetworkCreateDTO> networks) {
    return networkModelAssembler.toCollectionModel(networkService.createNetworks(networks));
  }

  @GetMapping("/networks/{id}")
  @Operation(summary = "Get a network by its id")
  public EntityModel<NetworkDTO> findById(@PathVariable Long id) {
    return networkModelAssembler.toModel(networkService.findNetworkById(id));
  }

  @DeleteMapping("/networks/{id}")
  @Operation(summary = "Delete a network by its id")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteById(@PathVariable Long id) {
    networkService.deleteNetworkById(id);
  }

  @PutMapping(
      value = "/networks/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaTypes.HAL_JSON_VALUE)
  @Operation(summary = "Update a network by its id")
  public EntityModel<NetworkDTO> update(
      @PathVariable Long id,
      @Validated(ValidationGroups.Update.class) @RequestBody NetworkCreateDTO networkDTO) {
    return networkModelAssembler.toModel(networkService.updateNetwork(id, networkDTO));
  }

  @GetMapping("/networks/{id}/resources")
  @Operation(summary = "List all resources of a network")
  public PagedModel<EntityModel<ResourceResponseModel>> getResources(
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return resourceModelAssembler.toPagedModel(
        (Page<ResourceResponseModel>)
            resourceService.findAllForNetwork(PageRequest.of(page, size), id),
        id);
  }

  @DeleteMapping("/networks/{id}/resources/{resourceId}")
  @Operation(summary = "Remove a resource from a network")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeResourceFromNetwork(
      @PathVariable Long id, @PathVariable("resourceId") Long resourceId) {
    networkService.removeResourceFromNetwork(id, resourceId);
  }

  @PostMapping(
      value = "/networks/{id}/resources",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Add a list of resources to a network")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addResourcesToNetwork(@PathVariable Long id, @RequestBody List<Long> resourceIds) {
    networkService.addResourcesToNetwork(id, resourceIds);
  }

  @GetMapping("/networks/{id}/managers")
  @Operation(summary = "List all managers of a network")
  public PagedModel<EntityModel<UserResponseModel>> getManagers(
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return userModelAssembler.toUnLinkedPagedModel(
        (Page<UserResponseModel>) personService.findAllForNetwork(PageRequest.of(page, size), id));
  }

  @PostMapping(
      value = "/networks/{id}/managers",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Add a list of managers to a network")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addManagersToNetwork(@PathVariable Long id, @RequestBody List<Long> managerIds) {
    networkService.addManagersToNetwork(id, managerIds);
  }

  @DeleteMapping("/networks/{id}/managers/{managerId}")
  @Operation(summary = "Remove a manager from a network")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeManagerFromNetwork(
      @PathVariable Long id, @PathVariable("managerId") Long managerId) {
    networkService.removeManagerFromNetwork(id, managerId);
  }

  @GetMapping("/networks/{id}/negotiations")
  @Operation(summary = "List all negotiations associated with a network")
  public PagedModel<EntityModel<NegotiationDTO>> getNegotiations(
      @PathVariable Long id, @Valid @Nullable @ParameterObject NegotiationFilterDTO filterDTO) {
    return negotiationModelAssembler.toPagedModel(
        (Page<NegotiationDTO>) negotiationService.findAllForNetwork(id, filterDTO),
        filterDTO,
        null);
  }

  @GetMapping("/networks/{id}/statistics")
  @Operation(
      summary = "Basic statistics about the network",
      description = "Provides basic statistics about requests linked to a Network.")
  public EntityModel<NetworkStatistics> getNetworkStatistics(@PathVariable Long id) {
    return EntityModel.of(networkStatisticsService.getBasicNetworkStats(id));
  }
}
