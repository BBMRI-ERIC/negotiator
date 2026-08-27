package eu.bbmri_eric.negotiator.negotiation.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.lifecycle.WellKnownNegotiationStates;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.dto.NegotiationCreateDTO;
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

    Converter<Enum<?>, String> negotiationStatusConverter = status -> nameOf(status.getSource());

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

    Converter<Boolean, NegotiationState> currentStateConverter =
        c -> NegotiationState.valueOf(initialStateFor(c.getSource()));
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
   * Reads the name off the Negotiation's current State, which is still an enum on the entity. The
   * empty string for an absent State is what the DTO has always carried, and {@code @JsonInclude}
   * would drop the field entirely from the response if it became a null.
   */
  private String nameOf(Enum<?> currentState) {
    if (Objects.isNull(currentState)) {
      return "";
    }
    return currentState.name();
  }

  /**
   * The State a Negotiation is created in. A name, like every other State in this class - the
   * {@code valueOf} at the call site above is the boundary translation, and exists only because the
   * entity's field is still typed as the enum. Slice 11 makes that field a name and deletes the
   * translation, leaving this method's result assigned directly.
   */
  private String initialStateFor(boolean isDraft) {
    return isDraft ? WellKnownNegotiationStates.DRAFT : WellKnownNegotiationStates.SUBMITTED;
  }

  private boolean publicPostsConverter(boolean isDraft) {
    return !isDraft;
  }
}
