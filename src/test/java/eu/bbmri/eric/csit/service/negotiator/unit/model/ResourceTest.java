package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import org.junit.jupiter.api.Test;

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
}
