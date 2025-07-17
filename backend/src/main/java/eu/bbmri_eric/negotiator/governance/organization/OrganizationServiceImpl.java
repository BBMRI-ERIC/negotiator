package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
  public OrganizationDTO findOrganizationById(Long id) {
    return modelMapper.map(
        organizationRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        OrganizationDTO.class);
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
    modelMapper.typeMap(OrganizationUpdateDTO.class, Organization.class)
        .addMappings(mapper -> mapper.skip(Organization::setId));
    modelMapper.map(updateDTO, organization);
    organization = organizationRepository.save(organization);
    return modelMapper.map(organization, OrganizationDTO.class);
  }
}
