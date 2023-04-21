package eu.bbmri.eric.csit.service.negotiator.configuration.mappers;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestModelsMapper {

  @Autowired
  ModelMapper modelMapper;

  @Value("${negotiator.frontend-url}")
  private String FRONTEND_URL;

  @PostConstruct
  void addMappings(){
    TypeMap<Request, RequestDTO> typeMap =
        modelMapper.createTypeMap(Request.class, RequestDTO.class);

    Converter<Set<Resource>, Set<ResourceDTO>> resourcesToResourcesDTO =
        q -> convertResourcesToResourcesDTO(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(resourcesToResourcesDTO)
                .map(Request::getResources, RequestDTO::setResources));

    Converter<String, String> requestToRedirectUrl = q -> convertIdToRedirectUrl(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper.using(requestToRedirectUrl).map(Request::getId, RequestDTO::setRedirectUrl));

    Converter<Negotiation, String> negotiationToNegotiationId = q -> convertNegotiationToNegotiationId(
        q.getSource());
    typeMap.addMappings(mapper ->
        mapper.using(negotiationToNegotiationId)
            .map(Request::getNegotiation, RequestDTO::setNegotiationId));
  }

  private String convertNegotiationToNegotiationId(Negotiation negotiation) {
    return negotiation != null ? negotiation.getId() : null;
  }

  private String convertIdToRedirectUrl(String requestId) {
    return "%s/requests/%s".formatted(FRONTEND_URL, requestId);
  }

  private Set<ResourceDTO> convertResourcesToResourcesDTO(Set<Resource> resources) {
    Map<String, ResourceDTO> parents = new HashMap<>();
    resources.forEach(
        collection -> {
          Resource parent = collection.getParent();
          ResourceDTO parentResource;
          if (parents.containsKey(parent.getSourceId())) {
            parentResource = parents.get(parent.getSourceId());
          } else {
            parentResource = new ResourceDTO();
            parentResource.setId(parent.getSourceId());
            parentResource.setType("biobank");
            parentResource.setChildren(new HashSet<>());
            parents.put(parent.getSourceId(), parentResource);
          }
          ResourceDTO collectionResource = new ResourceDTO();
          collectionResource.setType("collection");
          collectionResource.setId(collection.getSourceId());
          parentResource.getChildren().add(collectionResource);
        });

    return new HashSet<>(parents.values());
  }
}
