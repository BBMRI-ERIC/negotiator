package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.UserNotFoundException;
import eu.bbmri_eric.negotiator.service.RepresentativeNegotiationServiceImpl;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
public class RepresentativeNegotiationServiceTest {

  @Mock private ResourceRepository resourceRepository;

  @Mock private PersonRepository personRepository;

  @Mock private NegotiationRepository negotiationRepository;

  @Mock private ModelMapper modelMapper;

  @InjectMocks private RepresentativeNegotiationServiceImpl representativeNegotiationService;

  @Test
  public void test_retrieve_negotiations_valid_personId_and_resources() {
    // Arrange
    Pageable pageable = PageRequest.of(0, 10);
    Long personId = 1L;
    Person person = new Person();
    person.setId(personId);
    Resource resource1 = new Resource();
    resource1.setSourceId("resource1");
    Resource resource2 = new Resource();
    resource2.setSourceId("resource2");
    person.setResources(Set.of(resource1, resource2));

    Negotiation negotiation1 = new Negotiation();
    negotiation1.setId("negotiation1");
    negotiation1.setCurrentState(NegotiationState.IN_PROGRESS);

    Negotiation negotiation2 = new Negotiation();
    negotiation2.setId("negotiation2");
    negotiation2.setCurrentState(NegotiationState.ABANDONED);

    List<Negotiation> negotiations = List.of(negotiation1, negotiation2);
    List<NegotiationState> states =
        List.of(NegotiationState.IN_PROGRESS, NegotiationState.ABANDONED);
    Page<Negotiation> negotiationPage = new PageImpl<>(negotiations);

    when(personRepository.findById(personId)).thenReturn(Optional.of(person));
    when(negotiationRepository.findByResourceExternalIdsAndCurrentState(
            pageable, List.of("resource1", "resource2"), states))
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
}
