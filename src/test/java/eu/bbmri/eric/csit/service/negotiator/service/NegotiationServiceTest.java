package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class NegotiationServiceTest {

    @Mock
    NegotiationRepository negotiationRepository;
    @InjectMocks
    @Resource
    NegotiationService negotiationService;

    @BeforeEach
    void beforeAll() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void findByUserIDAndRoleReturnsNullForFakeParameters() {
        when(negotiationRepository.findByUserIdAndRole(any(), any())).thenReturn(null);
        assertNull(negotiationService.findByUserIdAndRole("fakeID", "fakeRole"));
    }
}
