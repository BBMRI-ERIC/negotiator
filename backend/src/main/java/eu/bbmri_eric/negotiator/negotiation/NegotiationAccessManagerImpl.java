package eu.bbmri_eric.negotiator.negotiation;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.user.PersonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NegotiationAccessManagerImpl implements NegotiationAccessManager {
  private final NegotiationRepository negotiationRepository;
  private final PersonRepository personRepository;

  public NegotiationAccessManagerImpl(
      NegotiationRepository negotiationRepository, PersonRepository personRepository) {
    this.negotiationRepository = negotiationRepository;
    this.personRepository = personRepository;
  }

  @Override
  public void verifyReadAccessForNegotiation(String negotiationId, Long userID) {
    if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userID)
        && !personRepository.isRepresentativeOfAnyResourceOfNegotiation(userID, negotiationId)
        && !personRepository.isManagerOfAnyResourceOfNegotiation(userID, negotiationId)) {
      throw new ForbiddenRequestException("You are not allowed to perform this action");
    }
  }

  @Override
  public void verifyUpdateAccessForNegotiation(String negotiationId, Long userID)
      throws ForbiddenRequestException {
    if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()
        && !negotiationRepository.existsByIdAndCreatedBy_Id(negotiationId, userID)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
  }
}
