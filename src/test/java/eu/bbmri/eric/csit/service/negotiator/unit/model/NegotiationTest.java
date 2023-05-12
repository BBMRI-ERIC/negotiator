package eu.bbmri.eric.csit.service.negotiator.unit.model;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class NegotiationTest {

    @Test
    void createNegotiation() {
        Negotiation negotiation = new Negotiation();
        assertInstanceOf(Negotiation.class, negotiation);
    }

    @Test
    void getNegotiationRequests() {
        Negotiation negotiation = new Negotiation();
        Request request = new Request();
        negotiation.setRequests(new HashSet<>(List.of(request)));
        assertEquals(1, negotiation.getRequests().size());
        assertEquals(request, negotiation.getRequests().iterator().next());
    }

    @Test
    void getNegotiationResources() {
        Negotiation negotiation = new Negotiation();
        Request request = new Request();
        Resource resource = new Resource();
        resource.setSourceId("fancyId");
        request.setResources(new HashSet<>(List.of(resource)));
        negotiation.setRequests(new HashSet<>(List.of(request)));
        assertEquals(new HashSet<>(List.of(resource)), negotiation.getAllResources());
    }
}
