package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "DefaultRequestService")
public class RequestServiceImpl implements RequestService {

  @Autowired private RequestRepository requestRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private DataSourceRepository dataSourceRepository;
  @Autowired private ModelMapper modelMapper;

  /**
   * Checks that resources in input conforms to the hierarchy regitered in the negotiator, and if
   * they do, add the leaf resources to the request
   *
   * @param resourceDTOs The List of Resources in the request negotiation
   * @param requestEntity The Request Entity to save in the DB
   */
  private void checkAndSetResources(Set<ResourceDTO> resourceDTOs, Request requestEntity) {
    Set<Resource> resourcesInQuery = new HashSet<>();
    resourceDTOs.forEach(
        resourceDTO -> {
          if (Objects.isNull(resourceRepository.findBySourceId(resourceDTO.getId()))) {
            throw new WrongRequestException(
                "Some of the specified resources were not found or the hierarchy was not correct");
          }else {
            resourcesInQuery.add(modelMapper.map(resourceDTO, Resource.class));
          }
        });
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
  public RequestDTO create(RequestCreateDTO requestBody) throws EntityNotStorableException {
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
  public RequestDTO findById(String id) throws EntityNotFoundException {
    Request request =
        requestRepository.findDetailedById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(request, RequestDTO.class);
  }

  public Set<RequestDTO> findAllById(Set<String> ids) {
    return ids.stream().map(this::findById).collect(Collectors.toSet());
  }

  @Transactional
  public RequestDTO update(String id, RequestCreateDTO queryRequest)
      throws EntityNotFoundException {
    Request requestEntity =
        requestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    Request request = saveRequest(queryRequest, requestEntity);
    return modelMapper.map(request, RequestDTO.class);
  }
}
