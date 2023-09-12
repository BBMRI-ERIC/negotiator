package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import org.junit.jupiter.api.Test;

public class OrganizationTest {
  @Test
  void initOrganization_nullId_NullPointerException() {
    assertThrows(
        NullPointerException.class,
        () -> {
          new Organization(null, null, null);
        });
  }

  @Test
  void initOrganization_ok() {
    Organization organization = new Organization("goodId", null, null);
    assertInstanceOf(Organization.class, organization);
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(new Organization("sameID", null, null), new Organization("sameID", null, null));
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(
        new Organization("sameID", null, null), new Organization("differentID", null, null));
  }

  @Test
  void getResources_oneResource_null() {
    assertNull(new Organization("id", null, null).getResources());
  }
}
