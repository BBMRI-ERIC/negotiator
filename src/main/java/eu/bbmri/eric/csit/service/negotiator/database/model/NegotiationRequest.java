package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationRequest {
    private String id;
    private String dataSourceId;
    private ArrayList<NegotiableEntity> negotiableEntities;
}
