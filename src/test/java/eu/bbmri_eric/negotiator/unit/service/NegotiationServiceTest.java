package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.model.Role;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.RoleRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.service.EmailService;
import eu.bbmri_eric.negotiator.service.NegotiationLifecycleService;
import eu.bbmri_eric.negotiator.service.NegotiationServiceImpl;
import eu.bbmri_eric.negotiator.service.ResourceLifecycleService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public class NegotiationServiceTest {
  @Mock NegotiationRepository negotiationRepository;
  @Mock RoleRepository roleRepository;
  @Mock PersonRepository personRepository;
  @Captor ArgumentCaptor<Specification<Negotiation>> specificationCaptor;
  @Captor ArgumentCaptor<Pageable> pageableCaptor;

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
                        .discoveryService(new DiscoveryService())
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

  @Disabled
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
    when(negotiationRepository.findAll(any(Specification.class))).thenReturn(List.of(negotiation));
    assertEquals(1, negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).size());
    assertEquals(
        NegotiationState.SUBMITTED.name(),
        negotiationService.findAllWithCurrentState(NegotiationState.SUBMITTED).get(0).getStatus());
  }

  @Test
  public void updatePostStatus_to_true() {
    Negotiation negotiation = buildNegotiation();
    String negotiationId = "biobank:1:collection:1";
    negotiation.setId(negotiationId);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));

    negotiation.setPostsEnabled(false);

    negotiationService.enablePosts(negotiationId);

    assertTrue(negotiation.getPostsEnabled());
  }

  @Test
  public void updatePostStatus_to_false() {
    Negotiation negotiation = buildNegotiation();
    String negotiationId = "biobank:1:collection:1";
    negotiation.setId(negotiationId);
    when(negotiationRepository.findById(any())).thenReturn(Optional.of(negotiation));

    negotiation.setPostsEnabled(true);

    negotiationService.disablePosts(negotiationId);

    assertFalse(negotiation.getPostsEnabled());
  }
}
