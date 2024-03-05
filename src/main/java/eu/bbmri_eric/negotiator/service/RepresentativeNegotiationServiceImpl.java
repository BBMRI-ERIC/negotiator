package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Person;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.NegotiationRepository;
import eu.bbmri_eric.negotiator.database.repository.PersonRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.UserNotFoundException;

import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class RepresentativeNegotiationServiceImpl implements RepresentativeNegotiationService {

  @Autowired ResourceRepository resourceRepository;
  @Autowired PersonRepository personRepository;
  @Autowired NegotiationRepository negotiationRepository;

  @Autowired ModelMapper modelMapper;

  @Override
  public Page<NegotiationDTO> findNegotiationsConcerningRepresentative(
      Pageable pageable, Long personId) {
    Person person =
        personRepository.findById(personId).orElseThrow(() -> new UserNotFoundException(personId));
    Page<Negotiation> negotiations =
        negotiationRepository.findByResourceExternalIdsAndCurrentState(
            pageable,
            person.getResources().stream().map(Resource::getSourceId).collect(Collectors.toList()),
            List.of(NegotiationState.IN_PROGRESS, NegotiationState.ABANDONED));
    return negotiations.map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class));
  }
}
