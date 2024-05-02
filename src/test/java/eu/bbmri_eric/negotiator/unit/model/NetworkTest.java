package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.model.Person;
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
  void addCollection() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.addResource(Resource.builder().build());
    assertEquals(1, network.getResources().size());
  }

  @Test
  void removeCollection() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    Resource resource = Resource.builder().build();
    network.addResource(resource);
    network.removeResource(resource);
    assertEquals(0, network.getResources().size());
  }

  @Test
  void addManager() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.addManager(Person.builder().build());
    assertEquals(1, network.getManagers().size());
  }

  @Test
  void removeManager() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    Person person = Person.builder().build();
    network.addManager(person);
    network.removeManager(person);
    assertEquals(0, network.getManagers().size());
  }
}
