package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.governance.network.Network;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
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
  void constructor_ok() {
    Network network = new Network("idk", "http://example.com");
    assertInstanceOf(Network.class, network);
    assertEquals("idk", network.getExternalId());
    assertEquals("http://example.com", network.getUri());
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(
        Network.builder().externalId("validId").build(),
        Network.builder().externalId("differentId").build());
  }

  @Test
  void addCollection() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    Resource resource = Resource.builder().build();
    network.addResource(resource);
    assertEquals(1, network.getResources().size());
    assertEquals(network, resource.getNetworks().stream().toList().get(0));
    assertEquals(resource, network.getResources().stream().toList().get(0));
  }

  @Test
  void removeCollection() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    Resource resource = Resource.builder().build();
    network.addResource(resource);
    network.removeResource(resource);
    assertEquals(0, network.getResources().size());
    assertEquals(0, resource.getNetworks().size());
  }
}
