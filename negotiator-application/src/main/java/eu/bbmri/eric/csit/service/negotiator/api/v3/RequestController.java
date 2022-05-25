package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.response.PersonRequestRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.response.RequestResponse;
import eu.bbmri.eric.csit.service.negotiator.model.Person;
import eu.bbmri.eric.csit.service.negotiator.model.PersonRequestRole;
import eu.bbmri.eric.csit.service.negotiator.model.Request;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class RequestController {

  private final RequestService requestService;
  private final ModelMapper modelMapper;

  public RequestController(RequestService requestService, ModelMapper modelMapper) {
    this.requestService = requestService;
    this.modelMapper = modelMapper;
    TypeMap<Request, RequestResponse> typeMap =
        modelMapper.createTypeMap(Request.class, RequestResponse.class);

    Converter<Set<PersonRequestRole>, Set<PersonRequestRoleDTO>> personsRoleConverter =
        prr -> personsRoleConverter(prr.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Request::getPersons, RequestResponse::setPersons));
  }

  private Set<PersonRequestRoleDTO> personsRoleConverter(Set<PersonRequestRole> personsRoles) {
    return personsRoles.stream()
        .map(
            personRole ->
                new PersonRequestRoleDTO(
                    personRole.getPerson().getAuthName(), personRole.getRole().getName()))
        .collect(Collectors.toSet());
  }

  /** Create a request and the project it belongs to */
  @PostMapping(
      value = "/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestResponse add(@Valid @RequestBody RequestRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Person creator = ((NegotiatorUserDetails) auth.getPrincipal()).getPerson();
    Request requestEntity = requestService.create(request, creator.getId());
    return modelMapper.map(requestEntity, RequestResponse.class);
  }

  /**
   * Create a request for a specific project
   *
   * @return RequestResponse
   */
  @PostMapping(
      value = "/projects/{projectId}/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestResponse add(@PathVariable Long projectId, @Valid @RequestBody RequestRequest request) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Person creator = ((NegotiatorUserDetails) auth.getPrincipal()).getPerson();
    Request requestEntity = requestService.create(projectId, request, creator.getId());
    return modelMapper.map(requestEntity, RequestResponse.class);
  }

  /**
   * Create a request for a specific project
   *
   * @param request
   * @return RequestResponse
   */
  @PutMapping(
      value = "/requests/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  RequestResponse update(@Valid @PathVariable Long id, @Valid @RequestBody RequestRequest request) {
    Request requestEntity = requestService.update(id, request);
    return modelMapper.map(requestEntity, RequestResponse.class);
  }

  @GetMapping("/requests")
  List<RequestResponse> list(
      @RequestParam(required = false) String biobankId,
      @RequestParam(required = false) String collectionId) {
    List<Request> requests;
    if (biobankId != null) {
      requests = requestService.findByBiobankId(biobankId);
    } else if (collectionId != null) {
      requests = requestService.findByCollectionId(collectionId);
    } else {
      requests = requestService.findAll();
    }
    return requests.stream()
        .map(request -> modelMapper.map(request, RequestResponse.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/requests/{id}")
  RequestResponse retrieve(@Valid @PathVariable Long id) {
    Request entity = requestService.findById(id);
    return modelMapper.map(entity, RequestResponse.class);
  }
}
