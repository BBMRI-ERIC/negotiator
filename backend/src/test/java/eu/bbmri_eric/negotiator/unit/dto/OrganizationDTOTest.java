package eu.bbmri_eric.negotiator.unit.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import org.junit.jupiter.api.Test;

public class OrganizationDTOTest {
  @Test
  public void testEquals_SameValues_ShouldReturnTrue() {
    OrganizationDTO org1 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    OrganizationDTO org2 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    assertEquals(org1, org2, "Objects with the same values should be equal");
  }

  @Test
  public void testEquals_DifferentValues_ShouldReturnFalse() {
    OrganizationDTO org1 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    OrganizationDTO org2 =
        OrganizationDTO.builder()
            .id(2L) // Different ID
            .externalId("ORG-54321") // Different externalId
            .name("Another Org") // Different name
            .description("A different description.")
            .contactEmail("contact@organization.org")
            .uri("https://another.org")
            .build();

    assertNotEquals(org1, org2, "Objects with different values should not be equal");
  }

  @Test
  public void testEquals_Null_ShouldReturnFalse() {
    OrganizationDTO org1 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    assertNotEquals(null, org1, "An object should not be equal to null");
  }

  @Test
  public void testEquals_SameObject_ShouldReturnTrue() {
    OrganizationDTO org =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    assertEquals(org, org, "An object should always be equal to itself");
  }

  @Test
  public void testHashCode_EqualObjects_ShouldReturnSameHashCode() {
    OrganizationDTO org1 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    OrganizationDTO org2 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    assertEquals(org1.hashCode(), org2.hashCode(), "Equal objects should have the same hash code");
  }

  @Test
  public void testHashCode_DifferentObjects_ShouldReturnDifferentHashCode() {
    OrganizationDTO org1 =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    OrganizationDTO org2 =
        OrganizationDTO.builder()
            .id(2L) // Different ID
            .externalId("ORG-54321") // Different externalId
            .name("Another Org") // Different name
            .description("A different description.")
            .contactEmail("contact@organization.org")
            .uri("https://another.org")
            .build();

    assertNotEquals(
        org1.hashCode(),
        org2.hashCode(),
        "Objects with different values should have different hash codes");
  }
}
