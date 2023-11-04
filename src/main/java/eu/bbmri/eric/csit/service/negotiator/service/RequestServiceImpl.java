package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.*;
import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisCollection;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.resource.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import jakarta.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service(value = "DefaultRequestService")
@CommonsLog
public class RequestServiceImpl implements RequestService {

  @Autowired private RequestRepository requestRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private DataSourceRepository dataSourceRepository;
  @Autowired private ModelMapper modelMapper;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private AccessCriteriaSetRepository accessCriteriaSetRepository;

  @Value("${negotiator.molgenis-url}")
  private String molgenisURL;

  private MolgenisService molgenisService = null;

  @PostConstruct
  public void init() {
    molgenisService = new MolgenisServiceImplementation(WebClient.create(molgenisURL));
  }

  @Transactional
  public RequestDTO create(RequestCreateDTO requestBody) throws EntityNotStorableException {
    Request request = new Request();
    request = saveRequest(requestBody, request);
    return modelMapper.map(request, RequestDTO.class);
  }

  @Transactional
  public RequestDTO update(String id, RequestCreateDTO requestBody) throws EntityNotFoundException {
    Request request =
        requestRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    request = saveRequest(requestBody, request);
    return modelMapper.map(request, RequestDTO.class);
  }

  private Request saveRequest(RequestCreateDTO requestCreateDTO, Request request) {
    request.setUrl(requestCreateDTO.getUrl());
    request.setHumanReadable(requestCreateDTO.getHumanReadable());
    request.setResources(getValidResources(requestCreateDTO.getResources()));
    request.setDataSource(getValidDataSource(requestCreateDTO.getUrl()));
    return requestRepository.save(request);
  }

  private Set<Resource> getValidResources(Set<ResourceDTO> resourceDTOs) {
    return resourceDTOs.stream()
        .map(
            resourceDTO ->
                findResourceById(resourceDTO.getId())
                    .orElseThrow(
                        () ->
                            new WrongRequestException(
                                "Some of the specified resources were not found.")))
        .collect(Collectors.toSet());
  }

  private Optional<Resource> findResourceById(String id) {
    Optional<Resource> resource = resourceRepository.findBySourceId(id);
    if (resource.isPresent()) {
      return resource;
    }
    return fetchResourceFromMolgenis(id);
  }

  private Optional<Resource> fetchResourceFromMolgenis(String id) {
    Optional<MolgenisCollection> molgenisCollection = molgenisService.findCollectionById(id);
    if (molgenisCollection.isPresent()) {
      Resource resource = modelMapper.map(molgenisCollection.get(), Resource.class);
      Organization organization =
          Organization.builder()
              .externalId(molgenisCollection.get().getBiobank().getId())
              .name(molgenisCollection.get().getBiobank().getName())
              .build();
      resource.setOrganization(organizationRepository.save(organization));
      resource.setDataSource(dataSourceRepository.findById(1L).get());
      resource.setAccessCriteriaSet(accessCriteriaSetRepository.findById(1L).get());
      // TODO: Fix primary key violation
      resourceRepository.save(resource);
      return Optional.of(resource);
    }
    return Optional.empty();
  }

  private DataSource getValidDataSource(String url) {
    URL dataSourceURL;
    try {
      dataSourceURL = new URL(url);
    } catch (MalformedURLException e) {
      throw new WrongRequestException("URL not valid");
    }
    return dataSourceRepository
        .findByUrl(String.format("%s://%s", dataSourceURL.getProtocol(), dataSourceURL.getHost()))
        .orElseThrow(() -> new WrongRequestException("Data source not found"));
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
}
