package eu.bbmri.eric.csit.service.negotiator.service;


import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class NegotiationServiceTest {

    @InjectMocks
    NegotiationService negotiationService;

    @Mock
    NegotiationRequest negotiationRequest;

    @Mock
    NegotiationRepository negotiationRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartingNegotiationReturnIsNotNull(){
        assertNotNull(negotiationService.startNegotiation(negotiationRequest));
    }

    @Test
    public void testGetNegotiationById() {
        Negotiation negotiation = Negotiation.builder().id(1).build();
        when(negotiationService.getNegotiationById(1)).thenReturn(negotiation);
        assertNotNull(negotiationService.getNegotiationById(1));
        assertNull(negotiationService.getNegotiationById(0));
    }
}
