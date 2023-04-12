package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetails;
import eu.bbmri.eric.csit.service.negotiator.configuration.auth.NegotiatorUserDetailsService;
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
        role -> personsRoleConverter(role.getSource());

    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Negotiation::getPersons, NegotiationDTO::setPersons));

    Converter<String, JsonNode> payloadConverter =
        p -> {
          try {
            return payloadConverter(p.getSource());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);  // TODO: raise the correct exception
          }
        };

    typeMap.addMappings(mapper -> mapper.using(payloadConverter)
        .map(Negotiation::getPayload, NegotiationDTO::setPayload));

  }

  private Set<PersonRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {
    return personsRoles.stream()
        .map(
            personRole ->
                new PersonRoleDTO(
                    personRole.getPerson().getAuthName(), personRole.getRole().getName()))
        .collect(Collectors.toSet());
  }

  private JsonNode payloadConverter(String jsonPayload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readTree(jsonPayload);
  }

  /**
   * Create a negotiation
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
      @RequestParam(required = false) String collectionId,
      @RequestParam(required = false) String userRole) {
    List<Negotiation> negotiations;
    if (biobankId != null) {
      negotiations = negotiationService.findByBiobankId(biobankId);
    } else if (collectionId != null) {
      negotiations = negotiationService.findByCollectionId(collectionId);
    } else if (userRole != null) {
      negotiations = negotiationService.findByUserIdAndRole(
              NegotiatorUserDetailsService.
                      getCurrentlyAuthenticatedUserId(),
              userRole);
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
