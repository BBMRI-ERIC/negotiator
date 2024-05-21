package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.exceptions.WrongRequestException;
import jakarta.annotation.PostConstruct;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
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
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private ModelMapper modelMapper;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private AccessFormRepository accessFormRepository;

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
    request.setDiscoveryService(getValidDiscoveryService(requestCreateDTO.getUrl()));
    return requestRepository.save(request);
  }

  private Set<Resource> getValidResources(Set<ResourceDTO> resourceDTOs) {
    return resourceDTOs.stream()
        .map(
            resourceDTO ->
                findResourceByExternalId(resourceDTO.getId())
                    .orElseThrow(
                        () ->
                            new WrongRequestException(
                                "Resource with source id: %s was not found and could not be fetched from the Discovery Service."
                                    .formatted(resourceDTO.getId()))))
        .collect(Collectors.toSet());
  }

  private Optional<Resource> findResourceByExternalId(String id) {
    Optional<Resource> resource = resourceRepository.findBySourceId(id);
    if (resource.isPresent()) {
      return resource;
    }
    log.info("Resource with ID %s not found in database. Fetching from Molgenis.".formatted(id));
    return fetchResourceFromMolgenis(id);
  }

  private Optional<Resource> fetchResourceFromMolgenis(String id) {
    Optional<MolgenisCollection> molgenisCollection = molgenisService.findCollectionById(id);
    if (molgenisCollection.isPresent()) {
      log.info(
          "Fetched resource with ID %s from the Discovery Service."
              .formatted(molgenisCollection.get().getId()));
      return persistAsResource(molgenisCollection);
    }
    log.error("Resource with ID %s was not found in the Discovery Service.".formatted(id));
    return Optional.empty();
  }

  private Optional<Resource> persistAsResource(Optional<MolgenisCollection> molgenisCollection) {
    Resource resource = prepareResourceForPersisting(molgenisCollection);
    resource = resourceRepository.save(resource);
    return Optional.of(resource);
  }

  private Resource prepareResourceForPersisting(Optional<MolgenisCollection> molgenisCollection) {
    Resource resource = modelMapper.map(molgenisCollection.get(), Resource.class);
    resource.setOrganization(getParentOrganization(molgenisCollection));
    resource.setDiscoveryService(discoveryServiceRepository.findById(1L).get());
    resource.setAccessForm(accessFormRepository.findById(1L).get());
    return resource;
  }

  private Organization getParentOrganization(Optional<MolgenisCollection> molgenisCollection) {
    if (!organizationRepository.existsByExternalId(molgenisCollection.get().getBiobank().getId())) {
      log.info(
          "Parent organization not found in database. Fetching from Molgenis and saving to DB.");
      return saveParentOrganization(molgenisCollection);
    } else {
      return organizationRepository
          .findByExternalId(molgenisCollection.get().getBiobank().getId())
          .get();
    }
  }

  @NonNull
  private Organization saveParentOrganization(Optional<MolgenisCollection> molgenisCollection) {
    Organization organization =
        Organization.builder()
            .externalId(molgenisCollection.get().getBiobank().getId())
            .name(molgenisCollection.get().getBiobank().getName())
            .build();
    organization = organizationRepository.save(organization);
    return organization;
  }

  private DiscoveryService getValidDiscoveryService(String url) {
    URL discoveryServiceURL;
    try {
      discoveryServiceURL = new URL(url);
    } catch (MalformedURLException e) {
      throw new WrongRequestException("URL not valid");
    }
    return discoveryServiceRepository
        .findByUrl(
            String.format(
                "%s://%s", discoveryServiceURL.getProtocol(), discoveryServiceURL.getHost()))
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
