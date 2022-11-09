package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Temp;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.repository.TempRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.TempRepositoryImpl;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
@Service
public class TempServiceImpl {

    private final TempRepository tempRepository = new TempRepositoryImpl();

    public Temp startNegotiation(NegotiationRequest negotiationRequest, Long creatorId){
        Temp temp = Temp
                .builder()
                .requests(new ArrayList<>(Collections.singletonList(negotiationRequest)))
                .creatorId(creatorId)
                .build();
        tempRepository.save(temp);
        return temp;
    }

    public Temp getNegotiationById(int id) {
        return tempRepository.findById(id);
    }
    public ArrayList<Temp> getAllNegotiations() {
        return tempRepository.findAll();
    }
}
