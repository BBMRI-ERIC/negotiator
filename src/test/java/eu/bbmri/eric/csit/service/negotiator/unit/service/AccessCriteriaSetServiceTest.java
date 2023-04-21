package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.repository.AccessCriteriaSetRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.service.AccessCriteriaSetServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

public class AccessCriteriaSetServiceTest {

  @Mock
  AccessCriteriaSetRepository accessCriteriaSetRepository;

  @Mock
  ModelMapper modelMapper;

  @InjectMocks
  AccessCriteriaSetServiceImpl service;

  private AutoCloseable closeable;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void afterAll() throws Exception {
    closeable.close();
  }

  @Test
  void testRaiseException_whenAccessCriteriaNotFound() {
    when(accessCriteriaSetRepository.findByResourceEntityId(any())).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class,
        () -> service.findByResourceEntityId("aResourceId"));

  }
}
