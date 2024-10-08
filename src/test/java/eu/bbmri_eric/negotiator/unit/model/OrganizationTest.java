package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import org.junit.jupiter.api.Test;

public class OrganizationTest {

  @Test
  void initOrganization_ok() {
    Organization organization = Organization.builder().externalId("validId").build();
    assertInstanceOf(Organization.class, organization);
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(
        Organization.builder().externalId("validId").build(),
        Organization.builder().externalId("validId").build());
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(
        Organization.builder().externalId("validId").build(),
        Organization.builder().externalId("differentId").build());
  }

  @Test
  void getResources_null_null() {
    assertNull(Organization.builder().externalId("validId").build().getResources());
  }
}
