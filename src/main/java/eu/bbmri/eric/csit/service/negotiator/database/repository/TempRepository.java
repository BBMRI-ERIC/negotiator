package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;

import java.util.ArrayList;

public interface TempRepository {
    Negotiation findById(int id);
    Negotiation save(Negotiation negotiation);

    ArrayList<Negotiation> findAll();
}
