package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;


@Getter
@Setter
@Builder
public class Negotiation {
    private int id;
    private ArrayList<NegotiationRequest> requests;

    public NegotiationRequest addRequest(NegotiationRequest negotiationRequest){
        this.requests.add(negotiationRequest);
        return negotiationRequest;
    }
}
