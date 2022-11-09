package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
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
public class RequestController {

  @Value("${negotiator.redirectPath:/v3/queries}")
  private String REDIRECT_PATH;

  private final RequestService requestService;

  private final ModelMapper modelMapper;

  public RequestController(RequestService requestService, ModelMapper modelMapper) {
    this.requestService = requestService;
    this.modelMapper = modelMapper;
    TypeMap<Request, RequestDTO> typeMap =
        modelMapper.createTypeMap(Request.class, RequestDTO.class);

    Converter<Set<Resource>, Set<ResourceDTO>> queryResourceToResources =
        q -> convertResourceToResources(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(queryResourceToResources)
                .map(Request::getResources, RequestDTO::setResources));

    Converter<String, String> queryToRedirectUrl = q -> convertIdToRedirectUrl(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper.using(queryToRedirectUrl).map(Request::getId, RequestDTO::setRedirectUrl));
  }

  private String convertIdToRedirectUrl(String queryId) {
    String baseURL = ServletUriComponentsBuilder.fromCurrentContextPath().toUriString();
    return "%s%s/%s".formatted(baseURL, REDIRECT_PATH, queryId);
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
  List<RequestDTO> list() {
    List<Request> queries = requestService.findAll();
    return queries.stream()
        .map(query -> modelMapper.map(query, RequestDTO.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/queries/{id}")
  RequestDTO retrieve(@PathVariable String id) {
    Request requestEntity = requestService.findById(id);
    return modelMapper.map(requestEntity, RequestDTO.class);
  }

  @PostMapping(
      value = "/queries",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestDTO add(@Valid @RequestBody RequestCreateDTO queryRequest) {
    Request requestEntity = requestService.create(queryRequest);
    return modelMapper.map(requestEntity, RequestDTO.class);
  }

  @PutMapping(
      value = "/queries/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  RequestDTO update(
      @Valid @PathVariable String id, @Valid @RequestBody RequestCreateDTO queryRequest) {
    Request requestEntity = requestService.update(id, queryRequest);
    return modelMapper.map(requestEntity, RequestDTO.class);
  }
}
