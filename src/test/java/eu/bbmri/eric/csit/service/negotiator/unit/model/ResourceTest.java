package eu.bbmri.eric.csit.service.negotiator.unit.model;

import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ResourceTest {
  @Test
  void initResource() {
    assertInstanceOf(Resource.class, new Resource());
  }

  @Test
  void testSetAndGetResourceID() {
    Resource resource = new Resource();
    resource.setSourceId("biobank:collection:1");
    assertEquals("biobank:collection:1", resource.getSourceId());
  }

  @Test
  void testAssertWithSameSourceIdEqual() {
    Resource resource = new Resource();
    resource.setSourceId("biobank:collection:1");
    Resource resource2 = new Resource();
    resource2.setSourceId("biobank:collection:1");
    assertEquals(resource, resource2);
  }

  @Test
  void testAssertWithDifferentIdsNotEqual() {
    Resource resource = new Resource();
    resource.setSourceId("biobank:collection:1");
    Resource resource2 = new Resource();
    resource2.setSourceId("biobank:collection:2");
    assertNotEquals(resource, resource2);
  }
}
