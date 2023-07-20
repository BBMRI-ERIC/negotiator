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
  void basicMapNegotiationToDTO() {
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
  void testStatusMapping() {
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
    assertEquals("APPROVED", negotiationDTO.getStatus());
  }

  @Test
  void testResourceStatesMappings() {
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
    JsonNode jsonNode = negotiationDTO.getResourceStatus();
    assertEquals("REPRESENTATIVE_CONTACTED", jsonNode.get("collection:1").textValue());
  }
}
