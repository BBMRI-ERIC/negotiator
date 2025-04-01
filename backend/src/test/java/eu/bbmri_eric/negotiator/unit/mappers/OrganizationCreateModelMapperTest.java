package eu.bbmri_eric.negotiator.unit.mappers;


import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationCreateDTO;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationCreateModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class OrganizationCreateModelMapperTest {

  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks OrganizationCreateModelMapper organizationCreateModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.organizationCreateModelMapper.addMappings();
  }

  @Test
  void createDtoToOrganization_map_ok() {
    OrganizationCreateDTO orgCreateDto =
        OrganizationCreateDTO.builder()
            .name("org")
            .description("organization")
            .contactEmail("org@test.org")
            .externalId("org_id")
            .uri("http://organization.test")
            .withdrawn(false)
            .build();
    Organization organization = mapper.map(orgCreateDto, Organization.class);
    assertEquals(organization.getName(), orgCreateDto.getName());
    assertEquals(organization.getDescription(), orgCreateDto.getDescription());
    assertEquals(organization.getContactEmail(), orgCreateDto.getContactEmail());
    assertEquals(organization.getExternalId(), orgCreateDto.getExternalId());
    assertEquals(organization.getUri(), orgCreateDto.getUri());
    assertEquals(organization.isWithdrawn(), orgCreateDto.getWithdrawn());
  }
}
