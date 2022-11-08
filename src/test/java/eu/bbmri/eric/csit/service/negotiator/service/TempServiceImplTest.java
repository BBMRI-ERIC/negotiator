package eu.bbmri.eric.csit.service.negotiator.service;


import eu.bbmri.eric.csit.service.negotiator.database.model.NegotiationRequest;
import eu.bbmri.eric.csit.service.negotiator.database.repository.TempRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;

public class TempServiceImplTest {

    @InjectMocks
    TempServiceImpl tempServiceImpl;

    @Mock
    NegotiationRequest negotiationRequest;

    @Mock
    TempRepository tempRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testStartingNegotiationReturnIsNotNull(){
        assertNotNull(tempServiceImpl.startNegotiation(negotiationRequest, 0L));
    }


    @Test
    public void testGetNegotiationById() {
        assertNotNull(tempServiceImpl.getAllNegotiations());
        assertNull(tempServiceImpl.getNegotiationById(0));
    }
}
