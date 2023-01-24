package eu.bbmri.eric.csit.service.negotiator.database.model;

import java.util.ArrayList;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Builder
public class Temp {

  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private Long creatorId;
  private ArrayList<NegotiationRequest> requests;

  public NegotiationRequest addRequest(NegotiationRequest negotiationRequest) {
    this.requests.add(negotiationRequest);
    return negotiationRequest;
  }
}
