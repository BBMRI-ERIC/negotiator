package eu.bbmri.eric.csit.service.negotiator.api.v2;

import eu.bbmri.eric.csit.service.negotiator.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryV2Request;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.response.QueryV2Response;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class QueryV2Controller {

  private static final String REDIRECT_PATH = "%s/researcher/query/%d";

  private final QueryService queryService;

  private final ModelMapper modelMapper;

  public QueryV2Controller(QueryService queryService, ModelMapper modelMapper) {
    this.queryService = queryService;
    this.modelMapper = modelMapper;

    // Mapper from v2 Query to V3 Query
    TypeMap<QueryV2Request, QueryRequest> v2ToV3Map =
        modelMapper.createTypeMap(QueryV2Request.class, QueryRequest.class);

    Converter<Set<CollectionV2DTO>, Set<ResourceDTO>> collectionV2ToResourceV3 =
        q -> convertCollectionV2ToResourceV3(q.getSource());

    v2ToV3Map.addMappings(
        mapper ->
            mapper
                .using(collectionV2ToResourceV3)
                .map(QueryV2Request::getCollections, QueryRequest::setResources));

    // Mapper from v2 Query to V3 Query
    TypeMap<Query, QueryV2Response> queryToV3Response =
        modelMapper.createTypeMap(Query.class, QueryV2Response.class);

    Converter<Long, String> queryToRedirectUri = q -> convertIdToRedirectUri(q.getSource());
    queryToV3Response.addMappings(
        mapper ->
            mapper.using(queryToRedirectUri).map(Query::getId, QueryV2Response::setRedirectUri));
  }

  private String convertIdToRedirectUri(Long queryId) {
    String baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    return REDIRECT_PATH.formatted(baseURL, queryId);
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
      value = "/api/directory/create_query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  QueryV2Response add(@Valid @RequestBody QueryV2Request queryRequest) {
    QueryRequest v3Request = modelMapper.map(queryRequest, QueryRequest.class);
    Query queryEntity = queryService.create(v3Request);
    return modelMapper.map(queryEntity, QueryV2Response.class);
  }
}
