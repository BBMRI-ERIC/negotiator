package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class NegotiableEntity {
    private String id;
    private String label;
    private String parentId;
}
