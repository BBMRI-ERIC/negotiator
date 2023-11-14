package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.List;

/** Service for managing negotiable Resource and their Representation. */
public interface ResourceRepresentativeService {
  boolean isRepresentativeOfResource(Long personId, String resourceExternalId)
      throws EntityNotFoundException;

  List<NegotiationDTO> findNegotiationsConcerningRepresentative(Long personId)
      throws EntityNotFoundException;

  List<Resource> getRepresentedResourcesForUser(Long personId) throws EntityNotFoundException;

  boolean isRepresentativeAny(Long personId, List<String> resourceIds);
}
