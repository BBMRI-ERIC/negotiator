package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NegotiableEntity {
    private String id;
    private String label;
    private String parentId;
}
