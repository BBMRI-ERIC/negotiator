package eu.bbmri.eric.csit.service.negotiator.mappers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationResourceLifecycleService;
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

  @Autowired private NegotiationResourceLifecycleService negotiationResourceLifecycleService;

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

    Converter<Map<String, NegotiationResourceState>, JsonNode> resourcesStatusConverter =
        resources -> {
          try {
            return resourcesStatusConverter(resources.getSource());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        };

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
                .using(resourcesStatusConverter)
                .map(Negotiation::getCurrentStatePerResource, NegotiationDTO::setResourceStatus));
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

  private JsonNode resourcesStatusConverter(
      Map<String, NegotiationResourceState> currentStatePerResource)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    if (Objects.isNull(currentStatePerResource)) {
      return JsonNodeFactory.instance.objectNode();
    }
    return mapper.valueToTree(currentStatePerResource);
  }
}
