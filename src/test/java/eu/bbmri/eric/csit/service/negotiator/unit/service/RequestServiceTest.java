package eu.bbmri.eric.csit.service.negotiator.unit.service;

import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class RequestServiceTest {

    @Mock RequestRepository requestRepository;

    @Spy ModelMapper modelMapper;
    @InjectMocks RequestService requestService = new RequestServiceImpl();

    private AutoCloseable closeable;

    @BeforeEach
    void before() {
        
        closeable = MockitoAnnotations.openMocks(this);
    }
    @Test
    void testGetAllOk(){
    when(requestRepository.findAll()).thenReturn(List.of(new Request()));
    assertEquals(1, requestService.findAll().size());
    }
}
