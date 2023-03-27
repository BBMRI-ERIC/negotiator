package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v3")
@CrossOrigin
public class NegotiationController {

  private final NegotiationService negotiationService;

  private final ModelMapper modelMapper;

  public NegotiationController(NegotiationService negotiationService, ModelMapper modelMapper) {
    this.negotiationService = negotiationService;
    this.modelMapper = modelMapper;
    TypeMap<Negotiation, NegotiationDTO> typeMap =
        modelMapper.createTypeMap(Negotiation.class, NegotiationDTO.class);

    Converter<Set<PersonNegotiationRole>, Set<PersonRoleDTO>> personsRoleConverter =
        prr -> personsRoleConverter(prr.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Negotiation::getPersons, NegotiationDTO::setPersons));

  }

  private Set<PersonRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {
    return personsRoles.stream()
        .map(
            personRole ->
                new PersonRoleDTO(
                    personRole.getPerson().getAuthName(), personRole.getRole().getName()))
        .collect(Collectors.toSet());
  }

  /**
   * Create a negotiation and the project it belongs to
   */
  @PostMapping(
      value = "/negotiations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@Valid @RequestBody NegotiationCreateDTO request) {
    Negotiation negotiationEntity = negotiationService.create(request, getCreatorId());
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }

  private Long getCreatorId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ((NegotiatorUserDetails) auth.getPrincipal()).getPerson().getId();
  }

  /**
   * Create a negotiation for a specific project
   *
   * @return NegotiationDTO
   */
  @PostMapping(
      value = "/projects/{projectId}/negotiations",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  NegotiationDTO add(@PathVariable String projectId,
      @Valid @RequestBody NegotiationCreateDTO request) {
    Negotiation negotiationEntity = negotiationService.create(projectId, request, getCreatorId());
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }

  /**
   * Create a negotiation for a specific project
   *
   * @return NegotiationDTO
   */
  @PutMapping(
      value = "/negotiations/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  NegotiationDTO update(@Valid @PathVariable String id,
      @Valid @RequestBody NegotiationCreateDTO request) {
    Negotiation negotiationEntity = negotiationService.update(id, request);
    return modelMapper.map(negotiationEntity, NegotiationDTO.class);
  }

  @GetMapping("/negotiations")
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

  @GetMapping("/negotiations/{id}")
  NegotiationDTO retrieve(@Valid @PathVariable String id) {
    Negotiation entity = negotiationService.findDetailedById(id);
    return modelMapper.map(entity, NegotiationDTO.class);
  }
}
