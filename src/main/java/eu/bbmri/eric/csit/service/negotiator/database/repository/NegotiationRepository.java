package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;

public interface NegotiationRepository {
    Negotiation findById(int id);
    Negotiation save(Negotiation negotiation);
}
