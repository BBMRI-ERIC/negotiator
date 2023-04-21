package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import java.util.List;

public interface NegotiationService {

  boolean exists(String negotiationId);

  NegotiationDTO create(NegotiationCreateDTO negotiationBody, Long creatorId);

  NegotiationDTO update(String negotiationId, NegotiationCreateDTO negotiationBody);

  NegotiationDTO addRequestToNegotiation(String negotiationId, String requestId);

  List<NegotiationDTO> findAll();

  NegotiationDTO findById(String id, boolean includeDetails);

  // TODO: change byBiobankId
  List<NegotiationDTO> findByBiobankId(String biobankId);

  // TODO: change to resouceId
  List<NegotiationDTO> findByCollectionId(String collectionId);

  List<NegotiationDTO> findByUserIdAndRole(String userId, String userRole);
}
