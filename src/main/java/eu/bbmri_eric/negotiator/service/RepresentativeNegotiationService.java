package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import org.springframework.data.domain.Pageable;

/** Service for managing negotiable Resource and their Representation. */
public interface RepresentativeNegotiationService {

  Iterable<NegotiationDTO> findNegotiationsConcerningRepresentative(
      Pageable pageable, Long personId) throws EntityNotFoundException;
}
