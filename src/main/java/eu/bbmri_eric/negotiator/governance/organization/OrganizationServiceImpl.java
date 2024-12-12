package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import eu.bbmri_eric.negotiator.governance.resource.dto.ResourceResponseModel;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationServiceImpl implements OrganizationService {
  @Autowired OrganizationRepository organizationRepository;

  ModelMapper modelMapper = new ModelMapper();

  @Override
  public OrganizationDTO findOrganizationById(Long id) {
    return modelMapper.map(
        organizationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        OrganizationDTO.class);
  }

  @Override
  public OrganizationDetailDTO findOrganizationDetailById(Long id) {
    Organization organization = organizationRepository.findDetailedById(id)
            .orElseThrow(() -> new EntityNotFoundException(id));

    List<ResourceResponseModel> resources = organization.getResources().stream()
            .map(resource -> ResourceResponseModel.builder()
                    .id(resource.getId())
                    .name(resource.getName())
                    .description(resource.getDescription())
                    .sourceId(resource.getSourceId())
                    .build())
            .collect(Collectors.toList());

    return OrganizationDetailDTO.builder()
            .id(organization.getId())
            .externalId(organization.getExternalId())
            .name(organization.getName())
            .resources(resources)
            .build();

  }

  @Override
  public OrganizationDTO findOrganizationByExternalId(String externalId) {
    return modelMapper.map(
        organizationRepository
            .findByExternalId(externalId)
            .orElseThrow(() -> new EntityNotFoundException(externalId)),
        OrganizationDTO.class);
  }

  @Override
  public Iterable<OrganizationDTO> findAllOrganizations(Pageable pageable) {
    return organizationRepository
        .findAll(pageable)
        .map(organization -> modelMapper.map(organization, OrganizationDTO.class));
  }

  @Override
  @Transactional
  public Iterable<OrganizationDTO> addOrganizations(Iterable<OrganizationCreateDTO> request) {
    ArrayList<Organization> organizations = new ArrayList<Organization>();
    for (OrganizationCreateDTO org : request) {
      Organization organization =
          Organization.builder()
              .name(org.getName())
              .externalId(org.getExternalId())
              .withdrawn(org.getWithdrawn())
              .build();
      organizations.add(organization);
    }
    List<Organization> savedOrganizations = organizationRepository.saveAll(organizations);
    return savedOrganizations.stream()
        .map(organization -> modelMapper.map(organization, OrganizationDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  public OrganizationDTO updateOrganizationById(Long id, OrganizationCreateDTO organization) {
    Organization org =
        organizationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    org.setName(organization.getName());
    org.setExternalId(organization.getExternalId());
    Organization updatedOrganization = organizationRepository.save(org);
    return modelMapper.map(updatedOrganization, OrganizationDTO.class);
  }
}
