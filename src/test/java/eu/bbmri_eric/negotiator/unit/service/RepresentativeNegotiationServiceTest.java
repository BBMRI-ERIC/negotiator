package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.*;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.UserNotFoundException;
import eu.bbmri_eric.negotiator.service.RepresentativeNegotiationServiceImpl;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class RepresentativeNegotiationServiceTest {

  @MockBean private PersonRepository personRepository;

  @MockBean private NegotiationRepository negotiationRepository;
  @Autowired private ModelMapper modelMapper;

  @Autowired private RepresentativeNegotiationServiceImpl representativeNegotiationService;

  private Pageable pageable;
  private Long personId;
  private Person person;
  private Resource resource1;
  private Resource resource2;
  private Negotiation negotiation1;
  private Negotiation negotiation2;
  private Page<Negotiation> negotiationPage;

  @BeforeEach
  void setUp() {
    pageable = PageRequest.of(0, 10);
    personId = 1L;

    person = new Person();
    person.setId(personId);

    Organization organization = new Organization();
    organization.setId(1L);
    organization.setExternalId("organization1");

    Request request = new Request();
    request.setId("request1");
    request.setUrl("http://test.edu");
    request.setDataSource(new DataSource());

    resource1 = createResource("resource1", organization);
    resource2 = createResource("resource2", organization);

    person.setResources(Set.of(resource1, resource2));

    request.setResources(Set.of(resource1, resource2));

    negotiation1 = createNegotiation("negotiation1", NegotiationState.IN_PROGRESS, Set.of(request));
    negotiation2 = createNegotiation("negotiation2", NegotiationState.ABANDONED, Set.of(request));

    List<Negotiation> negotiations = List.of(negotiation1, negotiation2);
    negotiationPage = new PageImpl<>(negotiations);
  }

  @Test
  public void test_retrieve_negotiations_valid_personId_and_resources() {
    List<NegotiationState> states =
        List.of(negotiation1.getCurrentState(), negotiation2.getCurrentState());

    when(personRepository.findById(personId)).thenReturn(Optional.of(person));
    when(negotiationRepository.findByResourceExternalIdsAndCurrentState(
            pageable,
            person.getResources().stream().map(Resource::getSourceId).collect(Collectors.toList()),
            states))
        .thenReturn(negotiationPage);

    Page<NegotiationDTO> negotiationDTOs =
        representativeNegotiationService.findNegotiationsConcerningRepresentative(
            pageable, personId);

    assertEquals(negotiationPage.getTotalElements(), negotiationDTOs.getTotalElements());
  }

  @Test
  public void test_throw_user_not_found_exception_invalid_personId() {
    Pageable pageable = PageRequest.of(0, 10);
    Long personId = 1L;

    when(personRepository.findById(personId)).thenReturn(Optional.empty());

    assertThrows(
        UserNotFoundException.class,
        () -> {
          representativeNegotiationService.findNegotiationsConcerningRepresentative(
              pageable, personId);
        });
  }

  private Negotiation createNegotiation(
      String id, NegotiationState currentState, Set<Request> requests) {
    Negotiation negotiation = new Negotiation();
    negotiation.setId(id);
    negotiation.setCurrentState(currentState);
    negotiation.setRequests(requests);
    negotiation.setCreationDate(LocalDateTime.now());
    negotiation.setModifiedDate(LocalDateTime.now());
    negotiation.setPostsEnabled(true);
    return negotiation;
  }

  private Resource createResource(String id, Organization organization) {
    Resource resource = new Resource();
    resource.setSourceId(id);
    resource.setOrganization(organization);
    resource.setName("ResourceName");
    return resource;
  }
}
