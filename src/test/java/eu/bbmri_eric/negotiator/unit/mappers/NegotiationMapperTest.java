package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.NegotiationResourceLifecycleRecord;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri_eric.negotiator.mappers.NegotiationModelMapper;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.HashSet;
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
                        .id(Long.valueOf(1))
                        .sourceId("collection:1")
                        .organization(
                            Organization.builder()
                                .name("Test Biobank")
                                .externalId("biobank:1")
                                .build())
                        .discoveryService(DiscoveryService.builder().build())
                        .build()))
            .build();

    Resource resource = Resource.builder().build();
    resource.setId(Long.valueOf(1));
    resource.setSourceId("collection:1");
    NegotiationResourceLifecycleRecord resourceLifecycleRecord =
        NegotiationResourceLifecycleRecord.builder()
            .changedTo(NegotiationResourceState.SUBMITTED)
            .resource(resource)
            .build();
    resourceLifecycleRecord.setCreationDate(LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00));
    Set<NegotiationResourceLifecycleRecord> records = new HashSet<>();
    records.add(resourceLifecycleRecord);
    Negotiation negotiation =
        Negotiation.builder()
            .requests(Set.of(request))
            .currentState(NegotiationState.SUBMITTED)
            .negotiationResourceLifecycleRecords(records)
            .build();
    negotiation.setCreationDate(LocalDateTime.of(2023, Month.SEPTEMBER, 19, 00, 00));
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
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    String status = negotiationDTO.getStatusForResource("collection:1");
    assertEquals("SUBMITTED", status);
  }
}
