package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceEvent;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import java.util.Set;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class NegotiationResourceLifecycleServiceImpl
    implements NegotiationResourceLifecycleService {

  @Override
  public void initializeTheStateMachine(String negotiationId, String resourceId) {}

  @Override
  public NegotiationResourceState getCurrentState(String negotiationId, String resourceId)
      throws EntityNotFoundException {return null;}

  @Override
  public Set<NegotiationResourceEvent> getPossibleEvents(String negotiationId, String resourceId)
      throws EntityNotFoundException {return null;}

  @Override
  public NegotiationResourceState sendEvent(
      String negotiationId, String resourceId, NegotiationResourceEvent negotiationEvent)
      throws WrongRequestException, EntityNotFoundException {return null;}
  
}
