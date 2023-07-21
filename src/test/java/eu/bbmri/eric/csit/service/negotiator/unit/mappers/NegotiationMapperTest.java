package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import eu.bbmri.eric.csit.service.negotiator.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.mappers.NegotiationModelMapper;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationLifecycleService;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationResourceLifecycleService;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class NegotiationMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @Mock NegotiationLifecycleService negotiationLifecycleService;
  @Mock NegotiationResourceLifecycleService negotiationResourceLifecycleService;

  @InjectMocks
  public NegotiationModelMapper negotiationModelMapper = new NegotiationModelMapper(mapper);

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.negotiationModelMapper.addMappings();
  }

  @Test
  void map_NegotiationToDTO_Ok() {
    when(negotiationLifecycleService.getCurrentState("newNegotiation"))
        .thenReturn(NegotiationState.APPROVED);
    when(negotiationResourceLifecycleService.getCurrentState("newNegotiation", "collection:1"))
        .thenReturn(NegotiationResourceState.REPRESENTATIVE_CONTACTED);
    Negotiation negotiation = new Negotiation();
    negotiation.setId("newNegotiation");
    Resource resource = new Resource();
    resource.setSourceId("collection:1");
    Request request = new Request();
    request.setResources(Set.of(resource));
    negotiation.setRequests(Set.of(request));
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
  void map_statePerResource_Ok() {
    Negotiation negotiation = buildNegotiation();
    negotiation.setStateForResource("collection:1", NegotiationResourceState.SUBMITTED);
    NegotiationDTO negotiationDTO = this.mapper.map(negotiation, NegotiationDTO.class);
    JsonNode jsonNode = negotiationDTO.getResourceStatus();
    assertEquals("SUBMITTED", jsonNode.get("collection:1").textValue());
  }

  private static Negotiation buildNegotiation() {
    Request request =
            Request.builder()
                    .resources(
                            Set.of(
                                    Resource.builder()
                                            .sourceId("collection:1")
                                            .dataSource(new DataSource())
                                            .build()))
                    .build();
    return Negotiation.builder()
            .requests(Set.of(request))
            .currentState(NegotiationState.SUBMITTED)
            .build();
  }
}
