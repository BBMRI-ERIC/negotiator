package eu.bbmri.eric.csit.service.negotiator.mappers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResources;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleService;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationResourceLifecycleService;
import java.util.HashMap;
import java.util.Map;
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

  @Autowired private NegotiationLifecycleService negotiationLifecycleService;

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

    Converter<String, String> negotiationStatusConverter =
        status -> negotiationStatusConverter(status.getSource());

    Converter<String, Boolean> negotiationPostsEnabledConverter =
        postsEnabled -> negotiationPostsEnabledConverter(postsEnabled.getSource());

    Converter<NegotiationResources, JsonNode> resourcesStatusConverter =
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
                .map(Negotiation::getId, NegotiationDTO::setStatus));
    typeMap.addMappings(
        mapper -> mapper.map(Negotiation::getPostsEnabled, NegotiationDTO::setPostsEnabled));
    typeMap.addMappings(
        mapper ->
            mapper
                .using(resourcesStatusConverter)
                .map(Negotiation::getAllResources, NegotiationDTO::setResourceStatus));
  }

  private Set<PersonRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {

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

  private String negotiationStatusConverter(String negotiationId) {
    try {
      return negotiationLifecycleService.getCurrentState(negotiationId).toString();
    } catch (EntityNotFoundException e) {
      return "";
    }
  }

  private Boolean negotiationPostsEnabledConverter(String negotiationId) {
    boolean postsEnabled = false;
    try {
      NegotiationState currentState = negotiationLifecycleService.getCurrentState(negotiationId);
      if (currentState.equals(NegotiationState.ONGOING)) {
        postsEnabled = true;
      }
    } catch (EntityNotFoundException e) {
      log.error("Current state not found for the provided negotiationId");
    }
    return postsEnabled;
  }

  private JsonNode resourcesStatusConverter(NegotiationResources negotiationResources)
      throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    Map<String, NegotiationResourceState> resourcesStatus = new HashMap<>();
    for (Resource resource : negotiationResources.getResources()) {
      try {
        resourcesStatus.put(
            resource.getSourceId(),
            negotiationResourceLifecycleService.getCurrentState(
                negotiationResources.getNegotiationId(), resource.getSourceId()));
      } catch (EntityNotFoundException e) {
        log.info("Negotiation and resource combination not found");
      }
    }
    log.debug(resourcesStatus);
    return mapper.valueToTree(resourcesStatus);
  }
}
