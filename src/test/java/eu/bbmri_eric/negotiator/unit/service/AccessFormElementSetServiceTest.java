package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.shared.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessCriteriaSetServiceImpl;
import eu.bbmri_eric.negotiator.form.AccessFormRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;

public class AccessFormElementSetServiceTest {

  @Mock AccessFormRepository accessFormRepository;

  @Mock ModelMapper modelMapper;

  @InjectMocks AccessCriteriaSetServiceImpl service;

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
    when(accessFormRepository.findByResourceId(any())).thenReturn(Optional.empty());
    assertThrows(EntityNotFoundException.class, () -> service.findByResourceId("aResourceId"));
  }
}
