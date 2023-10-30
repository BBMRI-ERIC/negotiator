package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.NegotiationModelMapper;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class NegotiationMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks
  public NegotiationModelMapper negotiationModelMapper = new NegotiationModelMapper(mapper);

  private static Negotiation buildNegotiation() {
    Request request =
        Request.builder()
            .resources(
                Set.of(
                    Resource.builder()
                        .sourceId("collection:1")
                        .organization(
                            Organization.builder()
                                .name("Test Biobank")
                                .externalId("biobank:1")
                                .build())
                        .dataSource(DataSource.builder().build())
                        .build()))
            .build();

    Negotiation negotiation =
        Negotiation.builder()
            .requests(Set.of(request))
            .currentState(NegotiationState.SUBMITTED)
            .build();
    negotiation.setCreationDate(LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00));
    negotiation.setStateForResource("collection:1", NegotiationResourceState.SUBMITTED);
    return negotiation;
  }

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.negotiationModelMapper.addMappings();
  }

  @Test
  void map_NegotiationToDTOid_Ok() {
    Negotiation negotiation = buildNegotiation();
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals(negotiation.getId(), negotiationDTO.getId());
  }

  @Test
  void map_currentState_ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setId("newNegotiation");
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals("SUBMITTED", negotiationDTO.getStatus());
  }

  @Test
  void map_entityToDtoPayload_throwsRuntimeException() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setPayload("Wrong json string");
    assertThrows(RuntimeException.class, () -> this.mapper.map(negotiation, NegotiationDTO.class));
  }

  @Test
  void map_personRoles_Ok() {
    Negotiation negotiation = buildNegotiation();
    PersonNegotiationRole personNegotiationRole =
        new PersonNegotiationRole(
            Person.builder()
                .authSubject("823")
                .authName("John")
                .authEmail("test@test.com")
                .id(1L)
                .build(),
            negotiation,
            new Role("CREATOR"));
    negotiation.setPersons(Set.of(personNegotiationRole));
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals("CREATOR", negotiationDTO.getPersons().iterator().next().getRole());
    assertEquals("John", negotiationDTO.getPersons().iterator().next().getName());
    assertEquals(String.valueOf(1L), negotiationDTO.getPersons().iterator().next().getId());
  }

  @Test
  void map_creationDate_ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setId("newNegotiation");
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    assertEquals(
        LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00), negotiationDTO.getCreationDate());
  }

  @Test
  void map_statePerResource_Ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setStateForResource("collection:1", NegotiationResourceState.SUBMITTED);

    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    String status = negotiationDTO.getStatusForResource("collection:1");
    assertEquals("SUBMITTED", status);
  }
}
