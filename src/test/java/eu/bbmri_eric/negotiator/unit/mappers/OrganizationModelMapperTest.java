package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationDTO;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationModelMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class OrganizationModelMapperTest {

  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks OrganizationModelMapper organizationModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.organizationModelMapper.addMappings();
  }

  @Test
  void organizationToDTO_map_Ok() {
    Organization organization =
        Organization.builder().name("Test Organizaiton").externalId("Organization_001").build();
    OrganizationDTO organizationDTO = mapper.map(organization, OrganizationDTO.class);
    assertEquals(organization.getExternalId(), organizationDTO.getExternalId());
    assertEquals(organization.getName(), organizationDTO.getName());
  }
}
