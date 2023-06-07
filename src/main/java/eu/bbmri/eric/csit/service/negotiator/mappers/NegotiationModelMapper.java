package eu.bbmri.eric.csit.service.negotiator.mappers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResources;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationStateService;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@CommonsLog
public class NegotiationModelMapper {

  @Autowired
  ModelMapper modelMapper;

  @Autowired
  NegotiationStateService negotiationStateService;


  @PostConstruct
  void addMappings() {
    TypeMap<Negotiation, NegotiationDTO> typeMap =
        modelMapper.createTypeMap(Negotiation.class, NegotiationDTO.class);

    Converter<Set<PersonNegotiationRole>, Set<PersonRoleDTO>> personsRoleConverter =
        role -> personsRoleConverter(role.getSource());

    Converter<String, String> negotiationStatusConverter = status -> negotiationStatusConverter(status.getSource());

    Converter<NegotiationResources, JsonNode> resourcesStatusConverter = resources -> {
      try {
        return resourcesStatusConverter(resources.getSource());
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
    };

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

    typeMap.addMappings(mapper -> mapper.using(negotiationStatusConverter).map(Negotiation::getId, NegotiationDTO::setStatus));
    typeMap.addMappings(mapper -> mapper.using(resourcesStatusConverter).map(Negotiation::getAllResources, NegotiationDTO::setResourceStatus));

  }

  private Set<PersonRoleDTO> personsRoleConverter(Set<PersonNegotiationRole> personsRoles) {
    return personsRoles.stream()
            .map(
                    personRole ->
                            new PersonRoleDTO(
                                    personRole.getPerson().getId().toString(),
                                    personRole.getPerson().getAuthName(),
                                    personRole.getRole().getName()))
            .collect(Collectors.toSet());
  }

  private JsonNode payloadConverter(String jsonPayload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    if (jsonPayload == null) {
      jsonPayload = "{}";
    }
    return mapper.readTree(jsonPayload);
  }

  private String negotiationStatusConverter(String negotiationId){
    try {
      return negotiationStateService.getCurrentState(negotiationId).toString();
    }
    catch (EntityNotFoundException e){
      return "";
    }
  }
  
  private JsonNode resourcesStatusConverter(NegotiationResources negotiationResources) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    Map<String, NegotiationState> resourcesStatus = new HashMap<>();
    for (Resource resource: negotiationResources.getResources() ) {
      try {
        resourcesStatus.put(resource.getSourceId(), negotiationStateService.getCurrentState(negotiationResources.getNegotiationId(), resource.getSourceId()));
      }
      catch (EntityNotFoundException e) {
        log.info("Negotiation and resource combination not found");
      }
    }
    log.debug(resourcesStatus.toString());
    return mapper.valueToTree(resourcesStatus);
  }
}
