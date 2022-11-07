package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@Builder
public class NegotiationRequest {
    private String id;
    private String dataSourceId;
    private ArrayList<NegotiableEntity> negotiableEntities;
}
