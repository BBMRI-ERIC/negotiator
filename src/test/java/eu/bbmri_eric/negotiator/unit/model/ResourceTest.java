package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import eu.bbmri_eric.negotiator.database.model.DataSource;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
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
        Resource.builder()
            .dataSource(new DataSource())
            .organization(
                Organization.builder().externalId("biobank:1").name("TestBiobank").build())
            .sourceId("resId")
            .build(),
        Resource.builder()
            .dataSource(new DataSource())
            .organization(
                Organization.builder().externalId("biobank:1").name("TestBiobank").build())
            .sourceId("resId")
            .build());
  }

  @Test
  void equals_differentSourceId_notEqual() {
    assertNotEquals(
        Resource.builder()
            .dataSource(new DataSource())
            .organization(
                Organization.builder().externalId("biobank:1").name("TestBiobank").build())
            .sourceId("resId")
            .build(),
        Resource.builder()
            .dataSource(new DataSource())
            .organization(
                Organization.builder().externalId("biobank:1").name("TestBiobank").build())
            .sourceId("resDiffId")
            .build());
  }
}
