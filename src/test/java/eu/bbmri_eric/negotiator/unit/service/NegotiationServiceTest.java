package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.DataSource;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.RoleRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.service.EmailService;
import eu.bbmri_eric.negotiator.service.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.service.NegotiationServiceImpl;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import java.util.Collections;
import java.util.List;
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
  @Mock EmailService emailService;
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
                        .organization(
                            Organization.builder()
                                .externalId("biobank:1")
                                .name("TestBiobank")
                                .build())
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
    when(negotiationRepository.findBySubjectIdAndRole(any(), any()))
        .thenReturn(Collections.emptyList());
    assertTrue(negotiationService.findByUserIdAndRole("fakeID", "fakeRole").isEmpty());
  }

  @Test
  void testGetNegotiationsForAListOFResources() {
    when(negotiationRepository.findByCollectionIds(any())).thenReturn(List.of(new Negotiation()));
    assertEquals(1, negotiationService.findByResourceIds(List.of("biobank:1:collection:1")).size());
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
