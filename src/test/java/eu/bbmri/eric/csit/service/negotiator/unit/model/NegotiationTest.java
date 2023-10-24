package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.negotiation.NegotiationState;
import eu.bbmri.eric.csit.service.negotiator.configuration.state_machine.resource.NegotiationResourceState;
import eu.bbmri.eric.csit.service.negotiator.database.model.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NegotiationTest {

  @Test
  void createNegotiation_ConstructorAndBuilder_Ok() {
    Negotiation negotiation = new Negotiation();
    assertInstanceOf(Negotiation.class, negotiation);
    Negotiation negotiationFromBuilder = Negotiation.builder().build();
    assertInstanceOf(Negotiation.class, negotiationFromBuilder);
  }

  @Test
  void getNegotiationRequests_Ok() {
    Negotiation negotiation = new Negotiation();
    Request request = new Request();
    negotiation.setRequests(new HashSet<>(List.of(request)));
    assertEquals(1, negotiation.getRequests().size());
    assertEquals(request, negotiation.getRequests().iterator().next());
  }

  @Test
  void getNegotiationResources_Ok() {
    Negotiation negotiation = new Negotiation();
    Request request = new Request();
    Resource resource = new Resource();
    resource.setSourceId("fancyId");
    request.setResources(new HashSet<>(List.of(resource)));
    negotiation.setRequests(new HashSet<>(List.of(request)));
    assertEquals(Set.of(resource), negotiation.getResources());
  }

  @Test
  void equals_sameIds_equal() {
    Negotiation negotiation = new Negotiation();
    negotiation.setId("sameId");
    Negotiation negotiation2 = new Negotiation();
    negotiation2.setId("sameId");
    assertEquals(negotiation, negotiation2);
  }

  @Test
  void equals_noIds_equal() {
    assertEquals(new Negotiation(), new Negotiation());
  }

  @Test
  void setCurrentState_Ok() {
    Negotiation negotiation =
        Negotiation.builder().currentState(NegotiationState.SUBMITTED).build();
    assertEquals(NegotiationState.SUBMITTED, negotiation.getCurrentState());
    negotiation.setCurrentState(NegotiationState.APPROVED);
    assertEquals(NegotiationState.APPROVED, negotiation.getCurrentState());
  }

  @Test
  void getCurrentStatesPerResource_defaultConstructor_isNull() {
    assertEquals(Map.of(), new Negotiation().getCurrentStatePerResource());
  }

  @Test
  void setResourcesStates_oneResource_Ok() {
    Negotiation negotiation = Negotiation.builder().build();
    negotiation.setStateForResource("collection:1", NegotiationResourceState.SUBMITTED);
    assertEquals(
        NegotiationResourceState.SUBMITTED,
        negotiation.getCurrentStatePerResource().get("collection:1"));
  }

  @Test
  void newNegotiationCurrentState_hasDefaultValue() {
    assertInstanceOf(NegotiationState.class, Negotiation.builder().build().getCurrentState());
  }

  @Test
  void getLifecycleHistory_newNegotiation_hasOneEntry() {
    Negotiation negotiation = Negotiation.builder().build();
    assertEquals(1, negotiation.getLifecycleHistory().size());
  }

  @Test
  void getLifeCycleHistory_newNegotiation_entryForSubmitted() {
    Negotiation negotiation = Negotiation.builder().build();
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiation.getLifecycleHistory().iterator().next().getChangedTo());
  }

  @Test
  void setCurrentState_correctly_updatesHistory() {
    Negotiation negotiation = Negotiation.builder().build();
    assertEquals(
        NegotiationState.SUBMITTED,
        negotiation.getLifecycleHistory().iterator().next().getChangedTo());
    negotiation.setCurrentState(NegotiationState.IN_PROGRESS);
    assertEquals(2, negotiation.getLifecycleHistory().size());
  }
}
