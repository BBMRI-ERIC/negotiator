package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.temp.NegotiationRequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.temp.NegotiationRequestDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRequestRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationService;
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
public class NegotiationController {

  private final NegotiationService negotiationService;

  private final ModelMapper modelMapper;

  public NegotiationController(NegotiationService negotiationService, ModelMapper modelMapper) {
    this.negotiationService = negotiationService;
    this.modelMapper = modelMapper;
    TypeMap<Negotiation, NegotiationDTO> typeMap =
        modelMapper.createTypeMap(Negotiation.class, NegotiationDTO.class);

    Converter<Set<PersonNegotiationRole>, Set<PersonRequestRoleDTO>> personsRoleConverter =
        prr -> personsRoleConverter(prr.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Negotiation::getPersons, NegotiationDTO::setPersons));

  }

  private Set<PersonRequestRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {
    return personsRoles.stream()
        .map(
            personRole ->
                new PersonRequestRoleDTO(
                    personRole.getPerson().getAuthName(), personRole.getRole().getName()))
        .collect(Collectors.toSet());
  }

  /** Create a negotiation and the project it belongs to */
  @PostMapping(
      value = "/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@Valid @RequestBody NegotiationCreateDTO request) {
    Negotiation negotiationEntity = negotiationService.create(request, getCreatorId());
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }
  @PostMapping(
          value = "/negotiation_requests",
          consumes = MediaType.APPLICATION_JSON_VALUE,
          produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationRequestDTO createRequest(@Valid @RequestBody NegotiationRequestCreateDTO requestCreateDTO) {
    NegotiationRequest negotiationRequest = convertToEntity(requestCreateDTO);
    negotiationService.createRequest(negotiationRequest);
    NegotiationRequestDTO negotiationRequestDTO = modelMapper.map(negotiationRequest, NegotiationRequestDTO.class);
    negotiationRequestDTO.setRedirectUrl("/gui/form/" + negotiationRequestDTO.getId());
    return negotiationRequestDTO;
  }
  @GetMapping("/negotiation_requests/{id}")
  NegotiationRequestDTO retrieve(@Valid @PathVariable Long id) {
    NegotiationRequest entity = negotiationService.getNegotiationRequestById(id);
    return modelMapper.map(entity, NegotiationRequestDTO.class);
  }

  private NegotiationRequest convertToEntity(NegotiationRequestCreateDTO requestCreateDTO) {
    return modelMapper.map(requestCreateDTO, NegotiationRequest.class);
  }

  private static Long getCreatorId() {
    return getCreator().getId();
  }

  private static Person getCreator() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorUserDetails) auth.getPrincipal()).getPerson();
  }

  /**
   * Create a negotiation for a specific project
   *
   * @return NegotiationDTO
   */
  @PostMapping(
      value = "/projects/{projectId}/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@PathVariable String projectId, @Valid @RequestBody NegotiationCreateDTO request) {
    Negotiation negotiationEntity = negotiationService.create(projectId, request, getCreatorId());
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }

  /**
   * Create a negotiation for a specific project
   *
   * @param request
   * @return NegotiationDTO
   */
  @PutMapping(
      value = "/requests/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  NegotiationDTO update(@Valid @PathVariable String id, @Valid @RequestBody NegotiationCreateDTO request) {
    Negotiation negotiationEntity = negotiationService.update(id, request);
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }

  @GetMapping("/requests")
  List<NegotiationDTO> list(
      @RequestParam(required = false) String biobankId,
      @RequestParam(required = false) String collectionId) {
    List<Negotiation> negotiations;
    if (biobankId != null) {
      negotiations = negotiationService.findByBiobankId(biobankId);
    } else if (collectionId != null) {
      negotiations = negotiationService.findByCollectionId(collectionId);
    } else {
      negotiations = negotiationService.findAll();
    }
    return negotiations.stream()
        .map(request -> modelMapper.map(request, NegotiationDTO.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/requests/{id}")
  NegotiationDTO retrieve(@Valid @PathVariable String id) {
    Negotiation entity = negotiationService.findDetailedById(id);
    return modelMapper.map(entity, NegotiationDTO.class);
  }
}
