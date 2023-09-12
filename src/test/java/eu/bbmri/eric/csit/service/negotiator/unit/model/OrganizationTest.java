package eu.bbmri.eric.csit.service.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri.eric.csit.service.negotiator.database.model.Organization;
import org.junit.jupiter.api.Test;

public class OrganizationTest {

  @Test
  void initOrganization_ok() {
    Organization organization = Organization.builder().id("validId").build();
    assertInstanceOf(Organization.class, organization);
  }

  @Test
  void equals_sameId_equal() {
    assertEquals(
        Organization.builder().id("validId").build(), Organization.builder().id("validId").build());
  }

  @Test
  void equals_differentId_notEqual() {
    assertNotEquals(
        Organization.builder().id("validId").build(),
        Organization.builder().id("differentId").build());
  }

  @Test
  void getResources_null_null() {
    assertNull(Organization.builder().id("validId").build().getResources());
  }
  
}
