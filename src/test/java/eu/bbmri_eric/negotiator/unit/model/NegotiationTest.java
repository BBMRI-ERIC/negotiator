package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.negotiation.Negotiation;
import eu.bbmri_eric.negotiator.negotiation.request.Request;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import eu.bbmri_eric.negotiator.negotiation.state_machine.resource.NegotiationResourceState;
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
  void getNegotiationResources_Ok() {
    Negotiation negotiation = new Negotiation();
    Resource resource = new Resource();
    resource.setSourceId("fancyId");
    negotiation.setResources(new HashSet<>(List.of(resource)));
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
