package eu.bbmri.eric.csit.service.negotiator.unit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import eu.bbmri.eric.csit.service.negotiator.database.repository.NegotiationRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.PersonRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RequestRepository;
import eu.bbmri.eric.csit.service.negotiator.database.repository.RoleRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationServiceImpl;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;

public class NegotiationServiceTest {

  @Mock
  NegotiationRepository negotiationRepository;
  @Mock
  RoleRepository roleRepository;
  @Mock
  PersonRepository personRepository;
  @Mock
  RequestRepository requestRepository;
  @Mock
  ModelMapper modelMapper;
  @Autowired
  @InjectMocks
  NegotiationServiceImpl negotiationService;
  private AutoCloseable closeable;

  @BeforeEach
  void before() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  void after() throws Exception {
    closeable.close();
  }

  @Test
  public void test_Exist_IsFalse_WhenNegotiationIsNotFound() {
    when(negotiationRepository.findById(any())).thenThrow(EntityNotFoundException.class);
    assertFalse(negotiationService.exists("unknown"));
  }

  @Test
  public void test_FindByUserAndRole_ReturnsEmptyList_whenNotFound() {
    when(negotiationRepository.findByUserIdAndRole(any(), any())).thenReturn(
        Collections.emptyList());
    assertTrue(negotiationService.findByUserIdAndRole("fakeID", "fakeRole").isEmpty());
  }
}
