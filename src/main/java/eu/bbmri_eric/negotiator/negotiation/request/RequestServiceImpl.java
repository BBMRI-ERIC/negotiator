package eu.bbmri_eric.negotiator.negotiation.request;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.form.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestDTO;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service(value = "DefaultRequestService")
@CommonsLog
public class RequestServiceImpl implements RequestService {

  @Autowired private RequestRepository requestRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private ModelMapper modelMapper;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private AccessFormRepository accessFormRepository;

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
    log.warn("Resource with ID %s not found in database.".formatted(id));
    throw new WrongRequestException(
        String.format("The resource with ID {0} cannot be founnd in the Negotiator", id));
  }

  private DiscoveryService getValidDiscoveryService(String url) {
    URL discoveryServiceURL;
    try {
      discoveryServiceURL = new URL(url);
    } catch (MalformedURLException e) {
      throw new WrongRequestException("URL is not valid");
    }
    return discoveryServiceRepository
        .findByUrl(
            String.format(
                "%s://%s", discoveryServiceURL.getProtocol(), discoveryServiceURL.getHost()))
        .orElseThrow(
            () ->
                new WrongRequestException(
                    "The Discovery Service from which this HTTP request originated is not registered in the Negotiator. Please contact Negotiator Administrators"));
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
