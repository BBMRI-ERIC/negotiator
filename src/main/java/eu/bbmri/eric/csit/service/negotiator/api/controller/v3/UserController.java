package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;


import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserModel;
import eu.bbmri.eric.csit.service.negotiator.mappers.UserModelAssembler;
import eu.bbmri.eric.csit.service.negotiator.service.PersonService;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@CommonsLog
public class UserController {
  @Autowired UserModelAssembler assembler;
  
  @Autowired PersonService personService;

  @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public PagedModel<EntityModel<UserModel>> listUsers(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {

    Iterable<UserModel> users = personService.findAll(page, size);
    if (users instanceof Page<UserModel>) {
      return assembler.toPagedModel((Page<UserModel>) users);
    }
    return PagedModel.empty();
  }

  @GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public EntityModel<UserModel> findById(@PathVariable Long id) {
    return assembler.toModel(personService.findById(id));
  }

  /**
   * Fetches Spring Roles for currently authenticated user.
   *
   * @return a List of roles.
   */
  @GetMapping(value = "/users/roles", produces = MediaType.APPLICATION_JSON_VALUE)
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
  @ResponseStatus(HttpStatus.OK)
  List<String> getRepresentedResources() {
    return personService
        .getResourcesRepresentedByUserId(
            NegotiatorUserDetailsService.getCurrentlyAuthenticatedUserInternalId())
        .stream()
        .map(Resource::getSourceId)
        .collect(Collectors.toList());
  }
}
