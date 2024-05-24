package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceSynchronizationJobRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.dto.MolgenisCollection;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.exception.DataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@CommonsLog
@Service
public class BBMRIDirectoryServiceSyncClientImpl implements BBMRIDirectoryServiceSynchClient {

  @Value("${negotiator.molgenis-url}")
  private String molgenisURL;

  private MolgenisService molgenisService = null;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private AccessFormRepository accessFormRepository;

  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private ResourceRepository resourceRepository;

  @Autowired
  private DiscoveryServiceSynchronizationJobRepository discoveryServiceSynchronizationJobRepository;

  private DiscoveryServiceSynchronizationJob jobRecord;

  @PostConstruct
  public void init() {
    molgenisService = new MolgenisServiceImplementation(WebClient.create(molgenisURL));
  }

  public void syncAllDiscoveryServiceObjects() {

    log.info("Started sync Directory recources at: " + new Date());
    log.info("Fetching all organizations... " + new Date());
    List<MolgenisBiobank> biobanks = molgenisService.findAllBiobanks();
    log.info("Adding missing organizations... " + new Date());
    addMissingOrganizationsAndResources(biobanks);
  }

  public void addMissingOrganizationsAndResources(List<MolgenisBiobank> directoryBiobanks) {
    DiscoveryService discoveryService =
        discoveryServiceRepository.findById(Long.valueOf("1")).get();
    AccessForm accessForm = accessFormRepository.findById(Long.valueOf("1")).get();
    for (MolgenisBiobank bb : directoryBiobanks) {
      String biobankId = bb.getId();
      Optional<Organization> organization = organizationRepository.findByExternalId(biobankId);
      if (organization.isEmpty()) {
        Organization newOrganization = addMissingOrganization(bb);
        addMissingCollections(newOrganization, bb, discoveryService, accessForm);

        List<MolgenisCollection> newBiobankCollections =
            molgenisService.findAllCollectionsByBiobankId(bb);

      } else {
        if (!bb.getName().equals(organization.get().getName())) {
          updateOrganizationName(organization.get(), bb);
        }
        List<MolgenisCollection> biobankCollections =
            molgenisService.findAllCollectionsByBiobankId(bb);
        for (MolgenisCollection c : biobankCollections) {
          String collectionId = c.getId();
          Optional<Resource> r = resourceRepository.findBySourceId(collectionId);
          if (r.isEmpty()) {
            log.info("Adding missing collection for the Biobank: " + bb.getId());
            addMissingCollections(organization.get(), bb, discoveryService, accessForm);
          } else {
            if (!r.get().getName().equals(c.getName())
                || !r.get().getDescription().equals((c.getDescription()))) {
              updateResourceNameAndDescription(r.get(), c);
            }
          }
        }
      }
    }
  }

  private Organization addMissingOrganization(MolgenisBiobank biobank) {
    log.info("Adding organization:" + biobank.getId());
    Organization newOrganization =
        Organization.builder().externalId(biobank.getId()).name(biobank.getName()).build();
    try {
      organizationRepository.save(newOrganization);
      return newOrganization;
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while adding missing organization");
      log.error(ex);
      throw new EntityNotStorableException();
    }
  }

  private void addMissingCollections(
      Organization organization,
      MolgenisBiobank biobank,
      DiscoveryService service,
      AccessForm form) {
    List<MolgenisCollection> newBiobankCollections =
        molgenisService.findAllCollectionsByBiobankId(biobank);
    for (MolgenisCollection collection : newBiobankCollections) {
      log.info("Adding collection:" + collection.getId());
      Resource newResource =
          Resource.builder()
              .organization(organization)
              .discoveryService(service)
              .accessForm(form)
              .sourceId(collection.getId())
              .name(collection.getName())
              .description(collection.getDescription())
              .build();
      try {
        resourceRepository.save(newResource);
      } catch (DataException | DataIntegrityViolationException ex) {
        log.error("Error while adding missing Collection as a resource");
        log.error(ex);
        throw new EntityNotStorableException();
      }
    }
  }

  private void updateOrganizationName(Organization organization, MolgenisBiobank biobank) {
    log.info(String.format("Updating name for existing organization {0}", biobank.getId()));
    organization.setName(biobank.getName());
    try {
      organizationRepository.save(organization);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while updating Organization name");
      log.error(ex);
      throw new EntityNotStorableException();
    }
  }

  private void updateResourceNameAndDescription(Resource resource, MolgenisCollection collection) {
    resource.setName(collection.getName());
    resource.setDescription(collection.getDescription());
    try {
      resourceRepository.save(resource);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while updating Resource name and description");
      log.error(ex);
      throw new EntityNotStorableException();
    }
  }
}
