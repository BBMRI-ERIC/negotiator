package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import org.junit.jupiter.api.Test;

public class OrganizationTest {
  @Test
  void initOrganization_ok() {
    Organization organization = new Organization("UniqueId");
    assertInstanceOf(Organization.class, organization);
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(new Organization("sameID"), new Organization("sameID"));
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(new Organization("sameID"), new Organization("differentID"));
  }
}
