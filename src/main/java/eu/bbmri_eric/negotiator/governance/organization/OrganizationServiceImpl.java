package eu.bbmri_eric.negotiator.governance.organization;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
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
          Organization.builder().name(org.getName()).externalId(org.getExternalId()).build();
      organizations.add(organization);
    }
    List<Organization> savedOrganizations = organizationRepository.saveAll(organizations);
    return OrganizationMapper.toDtoList(savedOrganizations);
  }
}
