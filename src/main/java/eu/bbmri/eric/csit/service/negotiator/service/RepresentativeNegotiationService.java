package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.List;

/** Service for managing negotiable Resource and their Representation. */
public interface RepresentativeNegotiationService {

  List<NegotiationDTO> findNegotiationsConcerningRepresentative(Long personId)
      throws EntityNotFoundException;
}
