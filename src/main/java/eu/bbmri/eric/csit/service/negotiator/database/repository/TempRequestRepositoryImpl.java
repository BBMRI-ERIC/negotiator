package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;

import java.util.HashMap;

public class TempRequestRepositoryImpl implements TempRequestRepository {
    private final HashMap<Long, NegotiationRequest> negotiationRequests = new HashMap<>();
    @Override
    public NegotiationRequest save(NegotiationRequest negotiationRequest) {
        negotiationRequests.put(negotiationRequest.getId(), negotiationRequest);
        return negotiationRequests.get(negotiationRequest.getId());
    }

    @Override
    public NegotiationRequest findById(Long id) {
        return negotiationRequests.get(id);
    }
}
