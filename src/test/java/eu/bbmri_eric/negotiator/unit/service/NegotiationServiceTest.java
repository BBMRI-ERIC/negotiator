package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.*;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.RoleRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.service.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.service.NegotiationServiceImpl;
import eu.bbmri_eric.negotiator.service.NotificationService;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

public class NegotiationServiceTest {

  @Mock NegotiationRepository negotiationRepository;
  @Mock RoleRepository roleRepository;
  @Mock PersonRepository personRepository;
  @Mock RequestRepository requestRepository;
  @Mock ModelMapper modelMapper;
  @Mock NegotiationLifecycleService negotiationLifecycleService;
  @Mock ResourceLifecycleService resourceLifecycleService;
  @Mock NotificationService notificationService;
  @InjectMocks NegotiationServiceImpl negotiationService;
  private AutoCloseable closeable;

  private static Negotiation buildNegotiation() {
    Request request =
        Request.builder()
            .resources(
                Set.of(
                    Resource.builder()
                        .sourceId("collection:1")
                        .dataSource(new DataSource())
                        .build()))
            .build();
    return Negotiation.builder()
        .requests(Set.of(request))
        .currentState(NegotiationState.SUBMITTED)
        .build();
  }

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
    when(notificationService.sendEmail(any(), any(), any())).thenReturn(true);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_Exist_IsFalse_WhenNegotiationIsNotFound() {
    when(negotiationRepository.findById(any())).thenThrow(EntityNotFoundException.class);
    assertFalse(negotiationService.exists("unknown"));
  }

  @Test
  public void test_FindByUserAndRole_ReturnsEmptyList_whenNotFound() {
    when(negotiationRepository.findByUserIdAndRole(any(), any()))
        .thenReturn(Collections.emptyList());
    assertTrue(negotiationService.findByUserIdAndRole("fakeID", "fakeRole").isEmpty());
  }

  @Test
  void testGetNegotiationsForAListOFResources() {
    when(negotiationRepository.findByCollectionIds(any())).thenReturn(List.of(new Negotiation()));
    assertEquals(1, negotiationService.findByResourceIds(List.of("biobank:1:collection:1")).size());
  }

  @Test
  void testCreateNegotiation() throws IOException {
    when(personRepository.findById(100L)).thenReturn(Optional.of(new Person()));
    when(roleRepository.findByName("ROLE_RESEARCHER")).thenReturn(Optional.of(new Role()));
    NegotiationCreateDTO negotiationCreateDTO = TestUtils.createNegotiation(Set.of("requestID"));
    Negotiation negotiation = new Negotiation();
    Request request = new Request();
    request.setResources(Set.of(new Resource()));
    negotiation.setRequests(Set.of(request));
    when(requestRepository.findAllById(Set.of("requestID"))).thenReturn(List.of(request));
    when(modelMapper.map(negotiationCreateDTO, Negotiation.class)).thenReturn(negotiation);
    NegotiationDTO savedDTO = new NegotiationDTO();
    savedDTO.setId("saved");
    when(negotiationRepository.save(negotiation)).thenReturn(negotiation);
    when(modelMapper.map(negotiation, NegotiationDTO.class)).thenReturn(savedDTO);
    NegotiationDTO negotiationDTO = negotiationService.create(negotiationCreateDTO, 100L);
    assertEquals("saved", negotiationDTO.getId());
  }

  @Test
  void findAllWithCurrentState_stateIsSubmitted_Ok() {
    Negotiation negotiation = buildNegotiation();
    NegotiationDTO negotiationDTO =
        NegotiationDTO.builder()
            .id(negotiation.getId())
            .status(negotiation.getCurrentState().name())
            .build();
    when(modelMapper.map(negotiation, NegotiationDTO.class)).thenReturn(negotiationDTO);
    when(negotiationRepository.findByCurrentState(NegotiationState.SUBMITTED))
        .thenReturn(List.of(negotiation));
    assertEquals(1, negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).size());
    assertEquals(
        NegotiationState.SUBMITTED.name(),
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).get(0).getStatus());
  }
}
