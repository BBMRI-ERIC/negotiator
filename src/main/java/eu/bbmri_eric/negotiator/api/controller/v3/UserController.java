package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.configuration.security.auth.NegotiatorUserDetailsService;
import eu.bbmri_eric.negotiator.dto.person.AssignResourceDTO;
import eu.bbmri_eric.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri_eric.negotiator.dto.person.UserInfoModel;
import eu.bbmri_eric.negotiator.dto.person.UserResponseModel;
import eu.bbmri_eric.negotiator.governance.network.NetworkDTO;
import eu.bbmri_eric.negotiator.governance.network.NetworkModelAssembler;
import eu.bbmri_eric.negotiator.governance.network.NetworkService;
import eu.bbmri_eric.negotiator.mappers.UserModelAssembler;
import eu.bbmri_eric.negotiator.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
@Tag(name = "Users", description = "Manage users and resources they represent")
@SecurityRequirement(name = "security_auth")
public class UserController {
  @Autowired UserModelAssembler assembler;

  @Autowired PersonService personService;
  @Autowired ModelMapper modelMapper;

  @Autowired NetworkService networkService;
  @Autowired NetworkModelAssembler networkModelAssembler;

  @GetMapping(value = "/users")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "List all users",
      description = "For filtering use for example: ?email=example@email.com")
  public PagedModel<EntityModel<UserResponseModel>> listUsers(
      @RequestParam(required = false) Map<String, String> filterProperty,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    if (NegotiatorUserDetailsService.getRoles().contains("ROLE_AUTHORIZATION_MANAGER")) {
      if (Objects.nonNull(filterProperty) && !filterProperty.isEmpty()) {
        return filteredPageModel(filterProperty, page, size);
      }
      return assembler.toPagedModel((Page<UserResponseModel>) personService.findAll(page, size));
    } else {
      return assembler.toPagedModel(
          personService.findById(
              NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId()));
    }
  }

  @GetMapping(value = "/userinfo")
  @Operation(summary = "Get information about the user based on the provided bearer token")
  public EntityModel<UserInfoModel> userInfo() {
    return assembler.toModel(
        personService.findById(
            NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId()),
        NegotiatorUserDetailsService.getRoles());
  }

  private PagedModel<EntityModel<UserResponseModel>> filteredPageModel(
      Map<String, String> filterProperty, int page, int size) {
    filterProperty.remove("page");
    filterProperty.remove("size");
    if (filterProperty.isEmpty()) {
      return assembler.toPagedModel((Page<UserResponseModel>) personService.findAll(page, size));
    } else {
      Iterable<UserResponseModel> users =
          personService.findAllByFilter(
              filterProperty.keySet().iterator().next(),
              filterProperty.values().iterator().next(),
              page,
              size);
      return assembler.toPagedModel((Page<UserResponseModel>) users);
    }
  }

  /**
   * Test description.
   *
   * @param id test param.
   * @return test return.
   */
  @GetMapping(value = "/users/{id}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "List all resources represented by a user")
  @ResponseStatus(HttpStatus.OK)
  public CollectionModel<ResourceResponseModel> findRepresentedResources(@PathVariable Long id) {
    return CollectionModel.of(personService.getResourcesRepresentedByUserId(id));
  }

  @PatchMapping(value = "/users/{id}/resources", consumes = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Add a resource to a user to represent")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void addResourceToRepresent(
      @PathVariable Long id, @RequestBody @Valid AssignResourceDTO resourceRequest) {
    personService.assignAsRepresentativeForResource(id, resourceRequest.getId());
  }

  @DeleteMapping(value = "/users/{id}/resources/{resourceId}")
  @Operation(summary = "Remove the user as a representative for a resource")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void removeAPersonAsARepresentativeForResource(
      @PathVariable Long id, @PathVariable Long resourceId) {
    personService.removeAsRepresentativeForResource(id, resourceId);
  }

  @GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Find a user by id")
  @ResponseStatus(HttpStatus.OK)
  public EntityModel<UserResponseModel> findById(@PathVariable Long id) {
    return assembler.toModel(personService.findById(id));
  }

  /**
   * Fetches Spring Roles for currently authenticated user.
   *
   * @return a List of roles.
   */
  @GetMapping(value = "/users/roles", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "Find roles of the currently authenticated user", deprecated = true)
  @ResponseStatus(HttpStatus.OK)
  List<String> getUserInfo() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map((obj) -> Objects.toString(obj, null))
        .collect(Collectors.toList());
  }

  /**
   * Returns the resources represented by the current user
   *
   * @return a List of resources id.
   */
  @GetMapping(value = "/users/resources", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      deprecated = true,
      summary = "Fetch external Ids of resources represented by the currently authenticated user")
  @ResponseStatus(HttpStatus.OK)
  List<String> getRepresentedResources() {
    return personService
        .getResourcesRepresentedByUserId(
            NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
        .stream()
        .map(ResourceResponseModel::getExternalId)
        .collect(Collectors.toList());
  }

  @GetMapping(value = "/users/{id}/networks", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(summary = "List all networks represented by a user")
  @ResponseStatus(HttpStatus.OK)
  public PagedModel<EntityModel<NetworkDTO>> getRepresentedNetworks(
      @PathVariable Long id,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return networkModelAssembler.toPagedModel(
        (Page<NetworkDTO>) networkService.findAllForManager(id, PageRequest.of(page, size)), id);
  }
}
