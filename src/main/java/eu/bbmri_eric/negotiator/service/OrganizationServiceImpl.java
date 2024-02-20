package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.dto.OrganizationDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
}
