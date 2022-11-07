package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class NegotiationService {

    private NegotiationRepository negotiationRepository;
    public Negotiation startNegotiation(NegotiationRequest negotiationRequest){
        Negotiation negotiation = Negotiation
                .builder()
                .requests(new ArrayList<>(Collections.singletonList(negotiationRequest)))
                .build();
        negotiationRepository.save(negotiation);
        return negotiation;
    }

    public Negotiation getNegotiationById(int id) {
        return negotiationRepository.findById(id);
    }
}
