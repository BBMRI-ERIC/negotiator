package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;


@Getter
@Setter
public class Negotiation {
    private int id;
    private ArrayList<NegotiationRequest> requests;
}
