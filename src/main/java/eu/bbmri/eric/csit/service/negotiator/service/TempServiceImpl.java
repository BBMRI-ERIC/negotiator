package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.repository.TempRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.TempRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
@Service
public class TempServiceImpl {

    private final TempRepository tempRepository = new TempRepositoryImpl();

    public Negotiation startNegotiation(NegotiationRequest negotiationRequest, Long creatorId){
        Negotiation negotiation = Negotiation
                .builder()
                .requests(new ArrayList<>(Collections.singletonList(negotiationRequest)))
                .creatorId(creatorId)
                .build();
        tempRepository.save(negotiation);
        return negotiation;
    }

    public Negotiation getNegotiationById(int id) {
        return tempRepository.findById(id);
    }
    public ArrayList<Negotiation> getAllNegotiations() {
        return tempRepository.findAll();
    }
}
