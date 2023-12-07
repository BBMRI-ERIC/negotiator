package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;


import eu.bbmri.eric.csit.service.negotiator.dto.person.ResourceResponseModel;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserResponseModel;
import eu.bbmri.eric.csit.service.negotiator.mappers.UserModelAssembler;
import eu.bbmri.eric.csit.service.negotiator.service.PersonService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
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
  @Autowired ModelMapper modelMapper;

  @GetMapping(value = "/users", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public PagedModel<EntityModel<UserResponseModel>> listUsers(
      @RequestParam(required = false) Map<String, String> filterProperty,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    if (Objects.nonNull(filterProperty) && !filterProperty.isEmpty()) {
      filterProperty.remove("page");
      filterProperty.remove("size");
      Iterable<UserResponseModel> users =
          personService.findAllByFilter(
              filterProperty.keySet().iterator().next(),
              filterProperty.values().iterator().next(),
              page,
              size);
      if (users instanceof Page<UserResponseModel>) {
        return assembler.toPagedModel((Page<UserResponseModel>) users);
      }
    }
    Iterable<UserResponseModel> users = personService.findAll(page, size);
    if (users instanceof Page<UserResponseModel>) {
      return assembler.toPagedModel((Page<UserResponseModel>) users);
    }
    return PagedModel.empty();
  }

  @GetMapping(value = "/users/{id}/resources", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  public CollectionModel<ResourceResponseModel> findRepresentedResources(@PathVariable Long id) {
    return CollectionModel.of(personService.getResourcesRepresentedByUserId(id));
  }

  @GetMapping(value = "/users/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
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
  @ResponseStatus(HttpStatus.OK)
  List<String> getUserInfo() {
    return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
        .map((obj) -> Objects.toString(obj, null))
        .collect(Collectors.toList());
  }
}
