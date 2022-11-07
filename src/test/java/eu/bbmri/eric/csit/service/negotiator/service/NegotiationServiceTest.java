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

import java.util.ArrayList;
import java.util.Collections;

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
        assertNotNull(negotiationService.startNegotiation(negotiationRequest, 0L));
    }


    @Test
    public void testGetNegotiationById() {
        assertNotNull(negotiationService.getAllNegotiations());
        assertNull(negotiationService.getNegotiationById(0));
    }
}
