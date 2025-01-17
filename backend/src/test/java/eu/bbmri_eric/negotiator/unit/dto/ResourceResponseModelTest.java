package eu.bbmri_eric.negotiator.unit.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import org.junit.jupiter.api.Test;

public class ResourceResponseModelTest {
  @Test
  void testConstructorWithRequiredFields() {
    // Arrange & Act
    ResourceResponseModel resource =
        new ResourceResponseModel(1L, "SRC-56789", "Clinical Data Repository");

    // Assert
    assertNotNull(resource);
    assertEquals(1L, resource.getId());
    assertEquals("SRC-56789", resource.getSourceId());
    assertEquals("Clinical Data Repository", resource.getName());
    assertEquals("", resource.getDescription());
    assertNull(resource.getContactEmail());
    assertNull(resource.getUri());
  }

  @Test
  void testConstructorWithAllFields() {
    // Arrange & Act
    ResourceResponseModel resource =
        new ResourceResponseModel(
            1L,
            "SRC-56789",
            "Clinical Data Repository",
            "A repository for clinical data.",
            "support@resource.org",
            "https://resource.org");

    // Assert
    assertNotNull(resource);
    assertEquals(1L, resource.getId());
    assertEquals("SRC-56789", resource.getSourceId());
    assertEquals("Clinical Data Repository", resource.getName());
    assertEquals("A repository for clinical data.", resource.getDescription());
    assertEquals("support@resource.org", resource.getContactEmail());
    assertEquals("https://resource.org", resource.getUri());
  }

  @Test
  void testSettersAndGetters() {
    // Arrange
    ResourceResponseModel resource = new ResourceResponseModel();

    // Act
    resource.setId(2L);
    resource.setSourceId("SRC-98765");
    resource.setName("Test Resource");
    resource.setDescription("A test description.");
    resource.setContactEmail("test@resource.org");
    resource.setUri("https://test.org");

    // Assert
    assertEquals(2L, resource.getId());
    assertEquals("SRC-98765", resource.getSourceId());
    assertEquals("Test Resource", resource.getName());
    assertEquals("A test description.", resource.getDescription());
    assertEquals("test@resource.org", resource.getContactEmail());
    assertEquals("https://test.org", resource.getUri());
  }

  @Test
  void testEqualsAndHashCode() {
    // Arrange
    ResourceResponseModel resource1 =
        new ResourceResponseModel(
            1L,
            "SRC-12345",
            "Resource Name",
            "Description",
            "email@example.com",
            "https://example.com");
    ResourceResponseModel resource2 =
        new ResourceResponseModel(
            1L,
            "SRC-12345",
            "Resource Name",
            "Description",
            "email@example.com",
            "https://example.com");
    ResourceResponseModel resource3 =
        new ResourceResponseModel(
            2L,
            "SRC-54321",
            "Another Resource",
            "Another Description",
            "another@example.com",
            "https://another.com");

    // Act & Assert
    assertEquals(resource1, resource2);
    assertNotEquals(resource1, resource3);
    assertEquals(resource1.hashCode(), resource2.hashCode());
    assertNotEquals(resource1.hashCode(), resource3.hashCode());
  }

  @Test
  void testDefaultDescriptionValue() {
    // Arrange
    ResourceResponseModel resource = new ResourceResponseModel();

    // Act & Assert
    assertEquals("", resource.getDescription());
  }

  @Test
  void testToString() {
    // Arrange
    ResourceResponseModel resource =
        new ResourceResponseModel(
            1L,
            "SRC-12345",
            "Resource Name",
            "Description",
            "email@example.com",
            "https://example.com");

    // Act
    String resourceString = resource.toString();

    // Assert
    assertTrue(resourceString.contains("id=1"));
    assertTrue(resourceString.contains("sourceId=SRC-12345"));
    assertTrue(resourceString.contains("name=Resource Name"));
    assertTrue(resourceString.contains("description=Description"));
    assertTrue(resourceString.contains("contactEmail=email@example.com"));
    assertTrue(resourceString.contains("uri=https://example.com"));
  }
}
