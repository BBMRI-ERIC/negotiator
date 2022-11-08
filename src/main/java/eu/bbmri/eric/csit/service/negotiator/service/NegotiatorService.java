package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import org.springframework.stereotype.Service;

@Service
public interface NegotiatorService {
    public Negotiation createNegotiation();
}
