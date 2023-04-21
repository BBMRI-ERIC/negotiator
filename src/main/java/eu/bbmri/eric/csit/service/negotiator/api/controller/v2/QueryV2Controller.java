package eu.bbmri.eric.csit.service.negotiator.api.controller.v2;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryCreateV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryV2Controller {

  private final RequestServiceImpl requestService;
  private final NegotiationServiceImpl negotiationService;
  private final ModelMapper modelMapper;
  @Value("${negotiator.frontend-url}")
  private String FRONTEND_URL;

  public QueryV2Controller(
      RequestServiceImpl requestService, NegotiationServiceImpl negotiationService,
      ModelMapper modelMapper) {
    this.requestService = requestService;
    this.negotiationService = negotiationService;
    this.modelMapper = modelMapper;

    // Mapper from v2 Request to V3 Request
    TypeMap<QueryCreateV2DTO, RequestCreateDTO> v2ToV3Map =
        modelMapper.createTypeMap(QueryCreateV2DTO.class, RequestCreateDTO.class);

    Converter<Set<CollectionV2DTO>, Set<ResourceDTO>> collectionV2ToResourceV3 =
        q -> convertCollectionV2ToResourceV3(q.getSource());

    v2ToV3Map.addMappings(
        mapper ->
            mapper
                .using(collectionV2ToResourceV3)
                .map(QueryCreateV2DTO::getCollections, RequestCreateDTO::setResources));

    // Mapper from v2 Request to V3 Request
    TypeMap<RequestDTO, QueryV2DTO> queryToV3Response =
        modelMapper.createTypeMap(RequestDTO.class, QueryV2DTO.class);

    Converter<String, String> queryToRedirectUri = q -> convertIdToRedirectUri(q.getSource());
    queryToV3Response.addMappings(
        mapper ->
            mapper.using(queryToRedirectUri).map(RequestDTO::getId, QueryV2DTO::setRedirectUri));
  }

  private String convertIdToRedirectUri(String queryId) {
    RequestDTO request = requestService.findById(queryId);
    String negotiationId = request.getNegotiationId();

    if (negotiationId == null) {
      return "%s/requests/%s".formatted(FRONTEND_URL, queryId);
    } else {
      return "%s/negotiations/%s/requests/%s".formatted(FRONTEND_URL, negotiationId, queryId);
    }
  }

  private Set<ResourceDTO> convertCollectionV2ToResourceV3(Set<CollectionV2DTO> collections) {
    Map<String, ResourceDTO> resources = new HashMap<>();
    collections.forEach(
        collection -> {
          String biobankId = collection.getBiobankId();

          ResourceDTO biobankResource;
          if (resources.containsKey(biobankId)) {
            biobankResource = resources.get(biobankId);
          } else {
            biobankResource = new ResourceDTO();
            biobankResource.setId(biobankId);
            biobankResource.setType("biobank");
            biobankResource.setChildren(new HashSet<>());
            resources.put(biobankId, biobankResource);
          }
          ResourceDTO collectionResource = new ResourceDTO();
          collectionResource.setType("collection");
          collectionResource.setId(collection.getCollectionId());
          biobankResource.getChildren().add(collectionResource);
        });

    return new HashSet<>(resources.values());
  }

  @PostMapping(
      value = "/directory/create_query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<QueryV2DTO> add(@Valid @RequestBody QueryCreateV2DTO queryRequest) {
    RequestCreateDTO v3Request = modelMapper.map(queryRequest, RequestCreateDTO.class);
    RequestDTO requestResponse;
    boolean created;
    if (queryRequest.getToken() != null && !queryRequest.getToken().isEmpty()) {
      // Update an old request or add a new one to a negotiation
      String[] tokens = queryRequest.getToken().split("__search__");
      // If the negotiation was not found in V2, a new request was created
      if (negotiationService.exists(tokens[0])) {
        created = false;
        if (tokens.length == 1) {
          requestResponse = requestService.create(v3Request);
          negotiationService.addRequestToNegotiation(tokens[0], requestResponse.getId());
        } else { // Updating an old request: the requestToken can be ignored
          requestResponse = requestService.update(tokens[1], v3Request);
        }
      } else {
        requestResponse = requestService.create(v3Request);
        created = true;
      }
    } else {
      requestResponse = requestService.create(v3Request);
      created = true;
    }
    QueryV2DTO response = modelMapper.map(requestResponse, QueryV2DTO.class);
    if (created) {
      return ResponseEntity.created(URI.create(response.getRedirectUri())).body(response);
    } else {
      return ResponseEntity.accepted().header("Location", response.getRedirectUri()).body(response);
    }
  }
}
