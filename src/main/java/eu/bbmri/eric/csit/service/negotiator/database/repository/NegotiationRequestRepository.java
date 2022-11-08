package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;

public interface NegotiationRequestRepository {
    public NegotiationRequest save(NegotiationRequest negotiationRequest);
    public NegotiationRequest findById(Long id);
}
