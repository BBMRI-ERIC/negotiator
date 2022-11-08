package eu.bbmri.eric.csit.service.negotiator.service;


import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

public class NegotiationServiceImplTest {

    @InjectMocks
    NegotiationServiceImpl negotiationServiceImpl;

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
        assertNotNull(negotiationServiceImpl.startNegotiation(negotiationRequest, 0L));
    }


    @Test
    public void testGetNegotiationById() {
        assertNotNull(negotiationServiceImpl.getAllNegotiations());
        assertNull(negotiationServiceImpl.getNegotiationById(0));
    }
}
