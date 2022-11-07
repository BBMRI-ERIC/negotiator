package eu.bbmri.eric.csit.service.negotiator.database.repository;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;

import java.util.ArrayList;
import java.util.HashMap;

public class NegotiationRepositoryImpl implements NegotiationRepository{

    private final HashMap<Integer, Negotiation> negotiations = new HashMap<>();
    @Override
    public Negotiation findById(int id) {
        return negotiations.get(id);
    }

    @Override
    public Negotiation save(Negotiation negotiation) {
        negotiations.put(negotiation.getId(), negotiation);
        return negotiation;
    }

    @Override
    public ArrayList<Negotiation> findAll() {
        return new ArrayList<>(negotiations.values());
    }
}
