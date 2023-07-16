package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.RequestModelsMapper;
import eu.bbmri.eric.csit.service.negotiator.mappers.ResourceModelMapper;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
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

  @Mock RequestRepository requestRepository;

  @Mock ResourceRepository resourceRepository;

  @Mock DataSourceRepository dataSourceRepository;

  @Spy ModelMapper modelMapper = new ModelMapper();
  @InjectMocks RequestService requestService = new RequestServiceImpl();

  @InjectMocks RequestModelsMapper requestModelsMapper;
  @InjectMocks ResourceModelMapper resourceModelMapper;

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
    when(dataSourceRepository.findByUrl(requestCreateDTO.getUrl())).thenReturn(Optional.of(new DataSource()));
    when(requestRepository.save(argThat(request -> request.getId() == null))).thenReturn(requestToBeSaved);
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
    when(dataSourceRepository.findByUrl(updatedRequestCreateDTO.getUrl())).thenReturn(Optional.of(new DataSource()));
    savedRequest.setHumanReadable(updatedRequestCreateDTO.getHumanReadable());
    when(requestRepository.save(argThat(request -> Objects.equals(request.getId(), savedRequest.getId()))))
            .thenReturn(savedRequest);
    RequestDTO updatedRequest = requestService.update(savedRequest.getId(), updatedRequestCreateDTO);
    assertEquals("Now I want nothing!", updatedRequest.getHumanReadable());
  }

  private static RequestCreateDTO buildRequestCreateDTO() {
    ResourceDTO resourceDTO = ResourceDTO.builder().id("test:collection").
            name("My collection").build();
    return RequestCreateDTO.builder()
        .url("https://directory.com")
        .humanReadable("I want everything")
        .resources(Set.of(resourceDTO))
        .build();
  }
}
