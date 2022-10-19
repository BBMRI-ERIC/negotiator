package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.response.QueryResponse;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/v3")
public class QueryController {

  @Value("${negotiator.redirectPath:/v3/queries}")
  private String REDIRECT_PATH;

  private final QueryService queryService;

  private final ModelMapper modelMapper;

  public QueryController(QueryService queryService, ModelMapper modelMapper) {
    this.queryService = queryService;
    this.modelMapper = modelMapper;
    TypeMap<Query, QueryResponse> typeMap =
        modelMapper.createTypeMap(Query.class, QueryResponse.class);

    Converter<Set<Resource>, Set<ResourceDTO>> queryResourceToResources =
        q -> convertResourceToResources(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(queryResourceToResources)
                .map(Query::getResources, QueryResponse::setResources));

    Converter<Long, String> queryToRedirectUrl = q -> convertIdToRedirectUrl(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper.using(queryToRedirectUrl).map(Query::getId, QueryResponse::setRedirectUrl));
  }

  private String convertIdToRedirectUrl(Long queryId) {
    String baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    return "%s%s/%d".formatted(baseURL, REDIRECT_PATH, queryId);
  }

  private Set<ResourceDTO> convertResourceToResources(Set<Resource> resources) {
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

  @GetMapping("/queries")
  List<QueryResponse> list() {
    List<Query> queries = queryService.findAll();
    return queries.stream()
        .map(query -> modelMapper.map(query, QueryResponse.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/queries/{id}")
  QueryResponse retrieve(@PathVariable Long id) {
    Query queryEntity = queryService.findById(id);
    return modelMapper.map(queryEntity, QueryResponse.class);
  }

  @PostMapping(
      value = "/queries",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  QueryResponse add(@Valid @RequestBody QueryRequest queryRequest) {
    Query queryEntity = queryService.create(queryRequest);
    return modelMapper.map(queryEntity, QueryResponse.class);
  }

  @PutMapping(
      value = "/queries/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  QueryResponse update(
      @Valid @PathVariable Long id, @Valid @RequestBody QueryRequest queryRequest) {
    Query queryEntity = queryService.update(id, queryRequest);
    return modelMapper.map(queryEntity, QueryResponse.class);
  }
}
