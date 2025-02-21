package eu.bbmri_eric.negotiator.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import java.util.HashSet;
import java.util.Set;
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

  @Test
  void testEqualsAndHashCode() {
    Set<Resource> resources = new HashSet<>();
    Organization org1 =
        Organization.builder()
            .id(1L)
            .externalId("ext123")
            .name("Test Org")
            .description("A test organization")
            .resources(resources)
            .contactEmail("test@example.com")
            .withdrawn(false)
            .uri("http://test.org")
            .build();

    Organization org2 =
        Organization.builder()
            .id(1L)
            .externalId("ext123")
            .name("Test Org")
            .description("A test organization")
            .resources(resources)
            .contactEmail("test@example.com")
            .withdrawn(false)
            .uri("http://test.org")
            .build();

    assertEquals(org1, org2);
    assertEquals(org1.hashCode(), org2.hashCode());
  }

  @Test
  void testNotEqualsDifferentExternalId() {
    Set<Resource> resources = new HashSet<>();
    Organization org1 =
        Organization.builder()
            .id(1L)
            .externalId("ext123")
            .name("Test Org")
            .description("A test organization")
            .resources(resources)
            .contactEmail("test@example.com")
            .withdrawn(false)
            .uri("http://test.org")
            .build();

    Organization org2 =
        Organization.builder()
            .id(1L)
            .externalId("ext456") // different externalId
            .name("Test Org")
            .description("A test organization")
            .resources(resources)
            .contactEmail("test@example.com")
            .withdrawn(false)
            .uri("http://test.org")
            .build();

    assertNotEquals(org1, org2);
  }

  @Test
  void testEqualsSameReference() {
    Organization org =
        Organization.builder()
            .id(1L)
            .externalId("ext123")
            .name("Test Org")
            .description("A test organization")
            .build();
    assertEquals(org, org);
  }

  @Test
  void testEqualsWithNullAndDifferentClass() {
    Organization org =
        Organization.builder()
            .id(1L)
            .externalId("ext123")
            .name("Test Org")
            .description("A test organization")
            .build();

    assertNotEquals(null, org);
    assertNotEquals("some string", org);
  }

  @Test
  void testDefaultWithdrawnValue() {
    // The builder should default withdrawn to false when not explicitly set
    Organization org =
        Organization.builder()
            .externalId("ext789")
            .name("Default Withdrawn Test")
            .description("Testing default withdrawn value")
            .build();
    assertFalse(org.isWithdrawn());
  }
}
