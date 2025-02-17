package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class OrganizationModelMapperTest {
  private ModelMapper modelMapper = new ModelMapper();

  @Test
  void shouldMapEntityToDTO() {
    // Arrange
    Organization organization =
        Organization.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .withdrawn(false)
            .build();

    // Act
    OrganizationDTO organizationDTO = modelMapper.map(organization, OrganizationDTO.class);

    // Assert
    assertNotNull(organizationDTO);
    assertEquals(organization.getId(), organizationDTO.getId());
    assertEquals(organization.getExternalId(), organizationDTO.getExternalId());
    assertEquals(organization.getName(), organizationDTO.getName());
    assertEquals(organization.getDescription(), organizationDTO.getDescription());
    assertEquals(organization.getContactEmail(), organizationDTO.getContactEmail());
    assertEquals(organization.getUri(), organizationDTO.getUri());
  }

  @Test
  void shouldMapDTOToEntity() {
    // Arrange
    OrganizationDTO organizationDTO =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name("BBMRI-ERIC")
            .description("A European research infrastructure.")
            .contactEmail("info@organization.org")
            .uri("https://organization.org")
            .build();

    // Act
    Organization organization = modelMapper.map(organizationDTO, Organization.class);

    // Assert
    assertNotNull(organization);
    assertEquals(organizationDTO.getId(), organization.getId());
    assertEquals(organizationDTO.getExternalId(), organization.getExternalId());
    assertEquals(organizationDTO.getName(), organization.getName());
    assertEquals(organizationDTO.getDescription(), organization.getDescription());
    assertEquals(organizationDTO.getContactEmail(), organization.getContactEmail());
    assertEquals(organizationDTO.getUri(), organization.getUri());
  }

  @Test
  void shouldHandleNullFieldsWhenMappingEntityToDTO() {
    // Arrange
    Organization organization =
        Organization.builder().id(1L).externalId("ORG-12345").name(null).description(null).build();

    // Act
    OrganizationDTO organizationDTO = modelMapper.map(organization, OrganizationDTO.class);

    // Assert
    assertNotNull(organizationDTO);
    assertEquals(organization.getId(), organizationDTO.getId());
    assertEquals(organization.getExternalId(), organizationDTO.getExternalId());
    assertNull(organizationDTO.getName());
    assertNull(organizationDTO.getDescription());
  }

  @Test
  void shouldHandleNullFieldsWhenMappingDTOToEntity() {
    // Arrange
    OrganizationDTO organizationDTO =
        OrganizationDTO.builder()
            .id(1L)
            .externalId("ORG-12345")
            .name(null)
            .description(null)
            .build();

    // Act
    Organization organization = modelMapper.map(organizationDTO, Organization.class);

    // Assert
    assertNotNull(organization);
    assertEquals(organizationDTO.getId(), organization.getId());
    assertEquals(organizationDTO.getExternalId(), organization.getExternalId());
    assertNull(organization.getName());
    assertNull(organization.getDescription());
  }
}
