package eu.bbmri.eric.csit.service.negotiator.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.dto.OrganizationDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.resource.ResourceWithStatusDTO;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
@CommonsLog
public class NegotiationModelMapper {

  @Autowired ModelMapper modelMapper;

  public NegotiationModelMapper(ModelMapper modelMapper) {
    this.modelMapper = modelMapper;
  }

  @PostConstruct
  public void addMappings() {
    TypeMap<Negotiation, NegotiationDTO> typeMap =
        modelMapper.createTypeMap(Negotiation.class, NegotiationDTO.class);

    Converter<Set<PersonNegotiationRole>, Set<PersonRoleDTO>> personsRoleConverter =
        role -> personsRoleConverter(role.getSource());

    Converter<NegotiationState, String> negotiationStatusConverter =
        status -> negotiationStatusConverter(status.getSource());

    Converter<Negotiation, Set<ResourceWithStatusDTO>> resourcesConverter =
        negotiation -> resourceConverter(negotiation.getSource());

    Converter<String, JsonNode> payloadConverter =
        p -> {
          try {
            return payloadConverter(p.getSource());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e); // TODO: raise the correct exception
          }
        };

    typeMap.addMappings(
        mapper ->
            mapper
                .using(personsRoleConverter)
                .map(Negotiation::getPersons, NegotiationDTO::setPersons));

    typeMap.addMappings(
        mapper ->
            mapper
                .using(payloadConverter)
                .map(Negotiation::getPayload, NegotiationDTO::setPayload));

    typeMap.addMappings(
        mapper ->
            mapper
                .using(negotiationStatusConverter)
                .map(Negotiation::getCurrentState, NegotiationDTO::setStatus));

    typeMap.addMappings(
        mapper ->
            mapper
                .using(resourcesConverter)
                .map(negotiation -> negotiation, NegotiationDTO::setResources));
  }

  private Set<ResourceWithStatusDTO> resourceConverter(Negotiation negotiation) {
    Set<Request> requests = negotiation.getRequests();
    final Map<String, NegotiationResourceState> statePerResource =
        negotiation.getCurrentStatePerResource();

    return requests.stream()
        .flatMap(
            request ->
                request.getResources().stream()
                    .map(resource -> buildResourceWithStatus(resource, statePerResource)))
        .collect(Collectors.toSet());
  }

  private ResourceWithStatusDTO buildResourceWithStatus(
      Resource resource, Map<String, NegotiationResourceState> statePerResource) {
    ResourceWithStatusDTO.ResourceWithStatusDTOBuilder builder =
        ResourceWithStatusDTO.builder()
            .id(resource.getSourceId())
            .name(resource.getName())
            .organization(modelMapper.map(resource.getOrganization(), OrganizationDTO.class));
    NegotiationResourceState state = statePerResource.get(resource.getSourceId());
    if (state != null) {
      builder.status(state.name());
    }
    return builder.build();
  }

  private Set<PersonRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {
    if (Objects.isNull(personsRoles)) {
      return null;
    }
    Stream<PersonRoleDTO> roles =
        personsRoles.stream()
            .map(
                personRole ->
                    new PersonRoleDTO(
                        String.valueOf(personRole.getPerson().getId()),
                        String.valueOf(personRole.getPerson().getAuthSubject()),
                        personRole.getPerson().getAuthName(),
                        personRole.getRole().getName()));
    return roles.collect(Collectors.toSet());
  }

  private JsonNode payloadConverter(String jsonPayload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    if (jsonPayload == null) {
      jsonPayload = "{}";
    }
    return mapper.readTree(jsonPayload);
  }

  private String negotiationStatusConverter(NegotiationState currentState) {
    if (Objects.isNull(currentState)) {
      return "";
    }
    return currentState.name();
  }
}
