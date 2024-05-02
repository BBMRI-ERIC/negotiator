package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Resource;
import org.junit.jupiter.api.Test;

class NetworkTest {

  @Test
  void initNetwork_ok() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    assertInstanceOf(Network.class, network);
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(
        Network.builder().externalId("validId").build(),
        Network.builder().externalId("validId").build());
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(
        Network.builder().externalId("validId").build(),
        Network.builder().externalId("differentId").build());
  }

  @Test
  void getManagers_null_null() {
    assertNull(Network.builder().externalId("validId").build().getManagers());
  }

  @Test
  void addCollection() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.addResource(Resource.builder().build());
    assertEquals(1, network.getResources().size());
  }
}
