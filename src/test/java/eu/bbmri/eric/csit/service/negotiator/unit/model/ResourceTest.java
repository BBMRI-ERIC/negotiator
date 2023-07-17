package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.*;

import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
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

  @Test
  void equals_sameSourceId_equal() {
    assertEquals(
        Resource.builder().dataSource(new DataSource()).sourceId("resId").build(),
        Resource.builder().dataSource(new DataSource()).sourceId("resId").build());
  }

  @Test
  void equals_differentSourceId_notEqual() {
    assertNotEquals(
        Resource.builder().dataSource(new DataSource()).sourceId("resId").build(),
        Resource.builder().dataSource(new DataSource()).sourceId("resDiffId").build());
  }
}
