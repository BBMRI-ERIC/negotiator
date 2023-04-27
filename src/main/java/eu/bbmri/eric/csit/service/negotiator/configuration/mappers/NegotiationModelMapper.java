package eu.bbmri.eric.csit.service.negotiator.configuration.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.PersonNegotiationRole;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NegotiationModelMapper {

  @Autowired
  ModelMapper modelMapper;

  @PostConstruct
  void addMappings() {
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
}
