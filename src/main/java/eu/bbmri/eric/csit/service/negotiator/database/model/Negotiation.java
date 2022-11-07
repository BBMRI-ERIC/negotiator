package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import java.util.ArrayList;


@Getter
@Setter
@Builder
public class Negotiation {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private Long creatorId;
    private ArrayList<NegotiationRequest> requests;

    public NegotiationRequest addRequest(NegotiationRequest negotiationRequest){
        this.requests.add(negotiationRequest);
        return negotiationRequest;
    }
}
