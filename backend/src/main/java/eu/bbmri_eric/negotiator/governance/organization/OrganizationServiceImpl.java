package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.common.AuthenticatedUserContext;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.common.exceptions.ForbiddenRequestException;
import eu.bbmri_eric.negotiator.governance.organization.dto.OrganizationFilterDTO;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@CommonsLog
public class OrganizationServiceImpl implements OrganizationService {
  OrganizationRepository organizationRepository;
  ModelMapper modelMapper;

  public OrganizationServiceImpl(
      OrganizationRepository organizationRepository, ModelMapper modelMapper) {
    this.organizationRepository = organizationRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  @Transactional
  public OrganizationDTO findOrganizationById(Long id, String expand) {
    Organization organization =
        organizationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    if (Objects.equals(expand, "resources")) {
      if (!AuthenticatedUserContext.isCurrentlyAuthenticatedUserAdmin()) {
        throw new ForbiddenRequestException("Only Administrators can view this attribute");
      }
      return modelMapper.map(organization, OrganizationWithResourcesDTO.class);
    }
    return modelMapper.map(organization, OrganizationDTO.class);
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
  public Iterable<OrganizationDTO> findAllOrganizations(OrganizationFilterDTO filters) {
    Specification<Organization> spec = OrganizationSpecificationBuilder.build(filters);
    Pageable pageable = PageRequest.of(filters.getPage(), filters.getSize());
    return organizationRepository
        .findAll(spec, pageable)
        .map(organization -> modelMapper.map(organization, OrganizationDTO.class));
  }

  @Override
  @Transactional
  public Iterable<OrganizationDTO> addOrganizations(Iterable<OrganizationCreateDTO> request) {
    ArrayList<Organization> organizations = new ArrayList<Organization>();
    for (OrganizationCreateDTO org : request) {
      Organization organization = modelMapper.map(org, Organization.class);
      organizations.add(organization);
    }
    List<Organization> savedOrganizations = organizationRepository.saveAll(organizations);
    return savedOrganizations.stream()
        .map(organization -> modelMapper.map(organization, OrganizationDTO.class))
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public OrganizationDTO updateOrganizationById(Long id, OrganizationUpdateDTO updateDTO) {
    Organization organization =
        organizationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    System.out.println(updateDTO.toString());
    modelMapper.getConfiguration().setSkipNullEnabled(true);
    modelMapper
        .typeMap(OrganizationUpdateDTO.class, Organization.class)
        .addMappings(mapper -> mapper.skip(Organization::setId));
    modelMapper.map(updateDTO, organization);
    organization = organizationRepository.save(organization);
    return modelMapper.map(organization, OrganizationDTO.class);
  }
}
