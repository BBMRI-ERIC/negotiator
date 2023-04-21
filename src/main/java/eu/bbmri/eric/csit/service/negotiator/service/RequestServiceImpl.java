package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestServiceImpl implements RequestService {

  private RequestRepository requestRepository;

  private ResourceRepository resourceRepository;

  private DataSourceRepository dataSourceRepository;

  private ModelMapper modelMapper;
  @Value("${negotiator.frontend-url}")
  private String FRONTEND_URL;

  public RequestServiceImpl(RequestRepository requestRepository,
      ResourceRepository resourceRepository, DataSourceRepository dataSourceRepository,
      ModelMapper modelMapper) {
    this.requestRepository = requestRepository;
    this.resourceRepository = resourceRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.modelMapper = modelMapper;

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

  /**
   * Checks that resources in input conforms to the hierarchy regitered in the negotiator, and if
   * they do, add the leaf resources to the request
   *
   * @param resourceDTOs The List of Resources in the request negotiation
   * @param requestEntity The Request Entity to save in the DB
   */
  private void checkAndSetResources(Set<ResourceDTO> resourceDTOs, Request requestEntity) {
    Set<Resource> resourcesInQuery = new HashSet<>();
    resourceDTOs.forEach(  // For each parent
        resourceDTO -> {
          // Gets the children
          Set<ResourceDTO> childrenDTOs = resourceDTO.getChildren();
          // Gets from the DB all the Resources with the ids of the children and parentId of the
          // parent
          Set<Resource> childrenResources =
              resourceRepository.findBySourceIdInAndParentSourceId(
                  childrenDTOs.stream().map(ResourceDTO::getId).collect(Collectors.toSet()),
                  resourceDTO.getId());
          // If the Resources in the DB are the same as the one in input, it means they are all correct
          if (childrenResources.size() < childrenDTOs.size()) {
            throw new WrongRequestException(
                "Some of the specified resources were not found or the hierarchy was not correct");
          } else {
            resourcesInQuery.addAll(childrenResources);
          }
        }
    );
    requestEntity.setResources(resourcesInQuery);
  }

  /**
   * Checks that the DataSource corresponding to the URL is present in the DB and adds it to the
   * Request entity
   *
   * @param url the url of the DataSource in the incoming request
   * @param requestEntity the Request entity to fill with the DataSource
   */
  private void checkAndSetDataSource(String url, Request requestEntity) {
    URL dataSourceURL;
    try {
      dataSourceURL = new URL(url);
    } catch (MalformedURLException e) {
      throw new WrongRequestException("URL not valid");
    }
    DataSource dataSource =
        dataSourceRepository
            .findByUrl(
                String.format("%s://%s", dataSourceURL.getProtocol(), dataSourceURL.getHost()))
            .orElseThrow(() -> new WrongRequestException("Data source not found"));
    requestEntity.setDataSource(dataSource);
  }

  private Request saveRequest(RequestCreateDTO queryRequest, Request requestEntity) {
    checkAndSetResources(queryRequest.getResources(), requestEntity);
    checkAndSetDataSource(queryRequest.getUrl(), requestEntity);
    requestEntity.setUrl(queryRequest.getUrl());
    requestEntity.setHumanReadable(queryRequest.getHumanReadable());
    return requestRepository.save(requestEntity);
  }

  @Transactional
  public RequestDTO create(RequestCreateDTO requestBody) {
    Request request = new Request();
    request = saveRequest(requestBody, request);
    return modelMapper.map(request, RequestDTO.class);
  }

  @Transactional(readOnly = true)
  public List<RequestDTO> findAll() {
    List<Request> requests = requestRepository.findAll();
    return requests.stream()
        .map(request -> modelMapper.map(request, RequestDTO.class))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public RequestDTO findById(String id) {
    Request request = requestRepository.findDetailedById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(request, RequestDTO.class);
  }

  public Set<RequestDTO> findAllById(Set<String> ids) {
    return ids.stream().map(this::findById).collect(Collectors.toSet());
  }

  @Transactional
  public RequestDTO update(String id, RequestCreateDTO queryRequest) {
    Request requestEntity =
        requestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    Request request = saveRequest(queryRequest, requestEntity);
    return modelMapper.map(request, RequestDTO.class);
  }
}
