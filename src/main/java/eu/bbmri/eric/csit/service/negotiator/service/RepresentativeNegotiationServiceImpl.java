package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ResourceRepository;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RepresentativeNegotiationServiceImpl implements RepresentativeNegotiationService {

  @Autowired ResourceRepository resourceRepository;
  @Autowired PersonRepository personRepository;
  @Autowired NegotiationRepository negotiationRepository;

  @Autowired ModelMapper modelMapper;

  @Override
  public List<NegotiationDTO> findNegotiationsConcerningRepresentative(Long personId) {
    Person person =
        personRepository
            .findDetailedById(personId)
            .orElseThrow(
                () -> new EntityNotFoundException("Person with id " + personId + " not found"));
    List<Negotiation> negotiations =
        negotiationRepository.findByResourceExternalIdsAndCurrentState(
            person.getResources().stream().map(Resource::getSourceId).collect(Collectors.toList()),
            NegotiationState.IN_PROGRESS);
    return negotiations.stream()
        .map(negotiation -> modelMapper.map(negotiation, NegotiationDTO.class))
        .collect(Collectors.toList());
  }
}
