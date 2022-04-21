package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.Biobank;
import eu.bbmri.eric.csit.service.model.Collection;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.response.QueryResponse;
import eu.bbmri.eric.csit.service.negotiator.service.QueryService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
public class QueryController {

  private final QueryService queryService;

  private final ModelMapper modelMapper;

  public QueryController(QueryService queryService, ModelMapper modelMapper) {
    this.queryService = queryService;
    this.modelMapper = modelMapper;
    TypeMap<Query, QueryResponse> typeMap =
        modelMapper.createTypeMap(Query.class, QueryResponse.class);

    typeMap.addMappings(
        mapper ->
            mapper
                .using(new QueryResourceConverted())
                .map(Query::getCollections, QueryResponse::setResources));
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
    Query queryEntity = queryService.getById(id);
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

  private static class QueryResourceConverted
      extends AbstractConverter<Set<Collection>, Set<ResourceDTO>> {

    @Override
    protected Set<ResourceDTO> convert(Set<Collection> collections) {
      Map<String, ResourceDTO> biobanks = new HashMap<>();
      collections.forEach(
          collection -> {
            Biobank b = collection.getBiobank();
            ResourceDTO biobankResource;
            if (biobanks.containsKey(b.getSourceId())) {
              biobankResource = biobanks.get(b.getSourceId());
            } else {
              biobankResource = new ResourceDTO();
              biobankResource.setId(b.getSourceId());
              biobankResource.setType("biobank");
              biobankResource.setChildren(new HashSet<>());
              biobanks.put(b.getSourceId(), biobankResource);
            }
            ResourceDTO collectionResource = new ResourceDTO();
            collectionResource.setType("collection");
            collectionResource.setId(collection.getSourceId());
            biobankResource.getChildren().add(collectionResource);
          });

      return new HashSet<>(biobanks.values());
    }
  }
}
