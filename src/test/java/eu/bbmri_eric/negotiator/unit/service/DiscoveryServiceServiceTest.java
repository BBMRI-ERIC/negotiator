package eu.bbmri_eric.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri_eric.negotiator.discovery_service.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.discovery_service.DiscoveryServiceCreateDTO;
import eu.bbmri_eric.negotiator.shared.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.discovery_service.DiscoveryServiceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;

public class DiscoveryServiceServiceTest {

  @Mock DiscoveryServiceRepository discoveryServiceRepository;

  @Mock ModelMapper modelMapper;

  @InjectMocks DiscoveryServiceServiceImpl service;

  private AutoCloseable closeable;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  private DiscoveryServiceCreateDTO getTestDTO() {
    return DiscoveryServiceCreateDTO.builder()
        .name("Name of the data source")
        .url("http://discoveryservice")
        .build();
  }

  @Test
  public void testCreateRaiseException_WhenDBFails() {
    DiscoveryServiceCreateDTO dto = getTestDTO();
    when(discoveryServiceRepository.save(any())).thenThrow(DataIntegrityViolationException.class);
    assertThrows(EntityNotStorableException.class, () -> service.create(dto));
  }
}
