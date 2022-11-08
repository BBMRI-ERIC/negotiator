package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import org.springframework.stereotype.Service;

@Service
public interface TempService {
    public Negotiation createNegotiation(NegotiationCreateDTO negotiationCreateDTO);
}
