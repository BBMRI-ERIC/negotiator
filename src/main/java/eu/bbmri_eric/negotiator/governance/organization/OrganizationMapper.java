package eu.bbmri_eric.negotiator.governance.organization;

import java.util.List;
import java.util.stream.Collectors;

public class OrganizationMapper {
  public static OrganizationDTO toDto(Organization organization) {
    OrganizationDTO orgDTO =
        OrganizationDTO.builder()
            .id(organization.getId())
            .name(organization.getName())
            .externalId(organization.getExternalId())
            .build();
    return orgDTO;
  }

  public static List<OrganizationDTO> toDtoList(List<Organization> organizations) {
    return organizations.stream().map(OrganizationMapper::toDto).collect(Collectors.toList());
  }
}
