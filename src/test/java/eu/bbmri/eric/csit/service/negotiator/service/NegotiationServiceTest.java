package eu.bbmri.eric.csit.service.negotiator.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
public class NegotiationServiceTest {
    private AutoCloseable closeable;

    @Mock
    NegotiationRepository negotiationRepository;

    @Autowired
    NegotiationServiceImpl negotiationService;

    @BeforeEach
    void beforeAll() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void afterAll() throws Exception {
        closeable.close();
    }

    @Test
    public void testExistIsFalse_WhenNegotiationIsNotFound() {
        when(negotiationRepository.findById(any())).thenThrow(EntityNotFoundException.class);
        assertFalse(negotiationService.exists("unknown"));
    }

    @Test
    public void findByUserIDAndRoleReturnsNullForFakeParameters() {
        when(negotiationRepository.findByUserIdAndRole(any(), any())).thenReturn(null);
        assertTrue(negotiationService.findByUserIdAndRole("fakeID", "fakeRole").isEmpty());
    }
}
