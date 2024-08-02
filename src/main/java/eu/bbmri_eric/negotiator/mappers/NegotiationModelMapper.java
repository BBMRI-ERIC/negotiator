package eu.bbmri_eric.negotiator.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.dto.OrganizationDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

  private Set<ResourceWithStatusDTO> resourceConverter(Negotiation negotiation) {
    Set<Request> requests = negotiation.getRequests();
    final Map<String, NegotiationResourceState> statePerResource =
        negotiation.getCurrentStatePerResource();

    return requests.stream()
        .flatMap(
            request ->
                request.getResources().stream()
                    .map(
                        resource ->
                            buildResourceWithStatus(
                                resource, statePerResource, negotiation.getId())))
        .collect(Collectors.toSet());
  }

  private ResourceWithStatusDTO buildResourceWithStatus(
      Resource resource,
      Map<String, NegotiationResourceState> statePerResource,
      String negotiationId) {
    ResourceWithStatusDTO.ResourceWithStatusDTOBuilder builder =
        ResourceWithStatusDTO.builder()
            .id(resource.getId())
            .externalId(resource.getSourceId())
            .negotiationId(negotiationId)
            .name(resource.getName())
            .organization(modelMapper.map(resource.getOrganization(), OrganizationDTO.class));
    NegotiationResourceState state = statePerResource.get(resource.getSourceId());
    if (state != null) {
      builder.status(state);
    }
    return builder.build();
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
