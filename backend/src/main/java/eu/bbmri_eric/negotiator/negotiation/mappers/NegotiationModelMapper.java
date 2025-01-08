package eu.bbmri_eric.negotiator.negotiation.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import jakarta.annotation.PostConstruct;
import java.util.Objects;
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

    Converter<NegotiationState, String> negotiationStatusConverter =
        status -> negotiationStatusConverter(status.getSource());

    Converter<String, JsonNode> payloadConverter =
        p -> {
          try {
            return payloadConverter(p.getSource());
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e); // TODO: raise the correct exception
          }
        };

    typeMap.addMappings(
        mapping -> mapping.map(Negotiation::getCreatedBy, NegotiationDTO::setAuthor));

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
