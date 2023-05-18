package eu.bbmri.eric.csit.service.negotiator.database.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class NegotiationResources {

    private String negotiationId;
    private List<Resource> resources;
}
