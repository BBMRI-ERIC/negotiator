package eu.bbmri_eric.negotiator.negotiation.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationDTO;
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

    Converter<String, String> negotiationStatusConverter =
        status -> statusTextFor(status.getSource());

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

    TypeMap<NegotiationCreateDTO, Negotiation> createDTOToEntity =
        modelMapper.createTypeMap(NegotiationCreateDTO.class, Negotiation.class);

    Converter<Boolean, String> currentStateConverter = c -> initialStateFor(c.getSource());
    createDTOToEntity.addMappings(
        mapper ->
            mapper
                .using(currentStateConverter)
                .map(NegotiationCreateDTO::isDraft, Negotiation::setCurrentState));

    Converter<Boolean, Boolean> publicPostsConverter = c -> publicPostsConverter(c.getSource());
    createDTOToEntity.addMappings(
        mapper ->
            mapper
                .using(publicPostsConverter)
                .map(NegotiationCreateDTO::isDraft, Negotiation::setPublicPostsEnabled));
  }

  private JsonNode payloadConverter(String jsonPayload) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    if (jsonPayload == null) {
      jsonPayload = "{}";
    }
    return mapper.readTree(jsonPayload);
  }

  /**
   * The text the DTO carries for the Negotiation's State: its name, or the empty string when it has
   * none.
   *
   * <p>The empty string is the whole reason this is not the identity function. It is what the wire
   * has always carried for a Negotiation with no State, and {@code @JsonInclude(NON_NULL)} on the
   * DTO would drop {@code status} from the response body entirely if this returned null.
   */
  private String statusTextFor(String currentState) {
    if (Objects.isNull(currentState)) {
      return "";
    }
    return currentState;
  }

  /** The State a Negotiation is created in. */
  private String initialStateFor(boolean isDraft) {
    return isDraft ? WellKnownNegotiationStates.DRAFT : WellKnownNegotiationStates.SUBMITTED;
  }

  private boolean publicPostsConverter(boolean isDraft) {
    return !isDraft;
  }
}
