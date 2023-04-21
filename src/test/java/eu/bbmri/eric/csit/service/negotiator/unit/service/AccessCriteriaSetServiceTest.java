package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.NegotiatorApplication;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AccessCriteriaSetRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.service.AccessCriteriaSetServiceImpl;
import java.util.Optional;
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
public class AccessCriteriaSetServiceTest {

  @Mock
  AccessCriteriaSetRepository accessCriteriaSetRepository;

  @Autowired
  AccessCriteriaSetServiceImpl accessCriteriaSetService;

  private AutoCloseable closeable;

  @BeforeEach
  void beforeAll() {
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
        () -> accessCriteriaSetService.findByResourceEntityId("aResourceId"));

  }
}
