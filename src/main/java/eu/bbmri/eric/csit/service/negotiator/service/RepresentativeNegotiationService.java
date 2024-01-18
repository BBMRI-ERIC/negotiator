package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import org.springframework.data.domain.Pageable;

/** Service for managing negotiable Resource and their Representation. */
public interface RepresentativeNegotiationService {

  Iterable<NegotiationDTO> findNegotiationsConcerningRepresentative(
      Pageable pageable, Long personId) throws EntityNotFoundException;
}
