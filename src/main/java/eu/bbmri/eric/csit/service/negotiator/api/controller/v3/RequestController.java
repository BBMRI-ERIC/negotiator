package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationRequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRequestRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
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
    TypeMap<Request, RequestDTO> typeMap =
        modelMapper.createTypeMap(Request.class, RequestDTO.class);

    Converter<Set<PersonRequestRole>, Set<PersonRequestRoleDTO>> personsRoleConverter =
        prr -> personsRoleConverter(prr.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Request::getPersons, RequestDTO::setPersons));
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
  RequestDTO add(@Valid @RequestBody RequestCreateDTO request) {
    Request requestEntity = requestService.create(request, getCreatorId());
    return modelMapper.map(requestEntity, RequestDTO.class);
  }
  @PostMapping(
          value = "/negotiation_requests",
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestDTO createRequest(@Valid @RequestBody NegotiationRequestCreateDTO request) {
    NegotiationRequest requestEntity = requestService.createRequest(request, getCreatorId());
    return modelMapper.map(requestEntity, RequestDTO.class);
  }

  private static Long getCreatorId() {
    return getCreator().getId();
  }

  private static Person getCreator() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorUserDetails) auth.getPrincipal()).getPerson();
  }

  /**
   * Create a request for a specific project
   *
   * @return RequestDTO
   */
  @PostMapping(
      value = "/projects/{projectId}/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestDTO add(@PathVariable String projectId, @Valid @RequestBody RequestCreateDTO request) {
    Request requestEntity = requestService.create(projectId, request, getCreatorId());
    return modelMapper.map(requestEntity, RequestDTO.class);
  }

  /**
   * Create a request for a specific project
   *
   * @param request
   * @return RequestDTO
   */
  @PutMapping(
      value = "/requests/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
      RequestDTO update(@Valid @PathVariable String id, @Valid @RequestBody RequestCreateDTO request) {
    Request requestEntity = requestService.update(id, request);
    return modelMapper.map(requestEntity, RequestDTO.class);
  }

  @GetMapping("/requests")
  List<RequestDTO> list(
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
        .map(request -> modelMapper.map(request, RequestDTO.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/requests/{id}")
  RequestDTO retrieve(@Valid @PathVariable String id) {
    Request entity = requestService.findDetailedById(id);
    return modelMapper.map(entity, RequestDTO.class);
  }
}
