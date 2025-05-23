package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.common.exceptions.WrongRequestException;
import eu.bbmri_eric.negotiator.discovery.DiscoveryService;
import eu.bbmri_eric.negotiator.discovery.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceModelMapper;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestDTO;
import eu.bbmri_eric.negotiator.negotiation.mappers.RequestModelsMapper;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.request.RequestRepository;
import eu.bbmri_eric.negotiator.negotiation.request.RequestServiceImpl;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

@CommonsLog
public class RequestServiceTest {

  @Mock private RequestRepository requestRepository;

  @Mock private ResourceRepository resourceRepository;

  @Mock private DiscoveryServiceRepository discoveryServiceRepository;

  @Spy private ModelMapper modelMapper = new ModelMapper();

  @InjectMocks private RequestServiceImpl requestService;

  @InjectMocks
  private RequestModelsMapper requestModelsMapper =
      new RequestModelsMapper("http://localhost:8080");

  @InjectMocks
  private ResourceModelMapper resourceModelMapper = new ResourceModelMapper(new ModelMapper());

  private static RequestCreateDTO buildRequestCreateDTO() {
    ResourceDTO resourceDTO =
        ResourceDTO.builder().id("test:collection").name("My collection").build();
    return RequestCreateDTO.builder()
        .url("https://directory.com")
        .humanReadable("I want everything")
        .resources(Set.of(resourceDTO))
        .build();
  }

  @BeforeEach
  void before() {
    MockitoAnnotations.openMocks(this);
    resourceModelMapper.addMappings();
    requestModelsMapper.addMappings();
  }

  @Test
  void getAll_ReturnsAll() {
    when(requestRepository.findAll()).thenReturn(List.of(new Request()));
    assertEquals(1, requestService.findAll().size());

    when(requestRepository.findAll()).thenReturn(List.of(new Request(), new Request()));
    assertEquals(2, requestService.findAll().size());
  }

  @Test
  void getById_Ok() {
    Request request = new Request();
    request.setId("newRequest");
    when(requestRepository.findDetailedById("newRequest")).thenReturn(Optional.of(request));
    assertEquals(request.getId(), requestService.findById(request.getId()).getId());
  }

  @Test
  void create_validParameters_Ok() {
    RequestCreateDTO requestCreateDTO = buildRequestCreateDTO();
    ResourceDTO resourceDTO = requestCreateDTO.getResources().iterator().next();
    Request requestToBeSaved = modelMapper.map(requestCreateDTO, Request.class);
    requestToBeSaved.setId("generatedByDB");
    Resource resourceToBeSaved = modelMapper.map(resourceDTO, Resource.class);

    when(resourceRepository.findBySourceId(resourceDTO.getId()))
        .thenReturn(Optional.of(resourceToBeSaved));
    when(discoveryServiceRepository.findByUrl(requestCreateDTO.getUrl()))
        .thenReturn(Optional.of(new DiscoveryService()));
    when(requestRepository.save(argThat(request -> request.getId() == null)))
        .thenReturn(requestToBeSaved);

    RequestDTO savedRequest = requestService.create(requestCreateDTO);
    assertEquals(requestToBeSaved.getId(), savedRequest.getId());
    assertEquals(requestCreateDTO.getHumanReadable(), savedRequest.getHumanReadable());
    assertEquals(resourceDTO.getId(), savedRequest.getResources().iterator().next().getId());
  }

  @Test
  void update_newHumanReadable_Ok() {
    Request savedRequest = modelMapper.map(buildRequestCreateDTO(), Request.class);
    savedRequest.setId("AlreadySavedRequest");
    RequestCreateDTO updatedRequestCreateDTO = buildRequestCreateDTO();
    updatedRequestCreateDTO.setHumanReadable("Now I want nothing!");

    when(requestRepository.findById("AlreadySavedRequest")).thenReturn(Optional.of(savedRequest));
    when(resourceRepository.findBySourceId(
            updatedRequestCreateDTO.getResources().iterator().next().getId()))
        .thenReturn(Optional.of(new Resource()));
    when(discoveryServiceRepository.findByUrl(updatedRequestCreateDTO.getUrl()))
        .thenReturn(Optional.of(new DiscoveryService()));

    // Mimic update on the saved request object
    savedRequest.setHumanReadable(updatedRequestCreateDTO.getHumanReadable());
    when(requestRepository.save(
            argThat(request -> Objects.equals(request.getId(), savedRequest.getId()))))
        .thenReturn(savedRequest);

    RequestDTO updatedRequest =
        requestService.update(savedRequest.getId(), updatedRequestCreateDTO);
    assertEquals("Now I want nothing!", updatedRequest.getHumanReadable());
  }

  @Test
  void create_unknownExternaiId_ko() {
    when(resourceRepository.findBySourceId("notexistingId")).thenReturn(Optional.empty());
    ResourceDTO resourceDTO =
        ResourceDTO.builder().id("notexistingId").name("not existing").build();

    RequestCreateDTO requestCreateDTO = buildRequestCreateDTO();
    requestCreateDTO.setResources(Set.of(resourceDTO));

    assertThrows(WrongRequestException.class, () -> requestService.create(requestCreateDTO));
  }
}
