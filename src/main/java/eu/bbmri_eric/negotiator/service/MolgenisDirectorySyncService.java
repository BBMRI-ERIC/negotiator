package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.DirectorySyncJobRecord;
import eu.bbmri_eric.negotiator.database.model.DirectorySyncJobState;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.DirectorySyncJobRecordRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@CommonsLog
public class MolgenisDirectorySyncService {

  @Value("${negotiator.molgenis-url}")
  private String molgenisURL;

  private MolgenisService molgenisService = null;

  private DirectorySyncJobRecord jobRecord;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private AccessFormRepository accessFormRepository;

  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private ResourceRepository resourceRepository;

  @Autowired private DirectorySyncJobRecordRepository directorySyncJobRecordRepository;

  @PostConstruct
  public void init() {
    molgenisService = new MolgenisServiceImplementation(WebClient.create(molgenisURL));
  }

  @Async
  public void syncDirectoryResources() {
    try {
      jobRecord = createJobRecord();
      jobRecord.setJobState(DirectorySyncJobState.IN_PROGRESS);
      directorySyncJobRecordRepository.save(jobRecord);
      log.info("Started sync Directory recources at: " + new Date());
      log.info("Fetching all organizations... " + new Date());
      List<MolgenisBiobank> biobanks = molgenisService.findAllBiobanks();
      log.info("Adding missing organizations... " + new Date());
      addMissingOrganizationsAndResources(biobanks);
      log.info("Job successfully completed");
      jobRecord.setJobState(DirectorySyncJobState.COMPLETED);
      directorySyncJobRecordRepository.save(jobRecord);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while updating Directory sync job record");
      log.error(ex);
      jobRecord.setJobState(DirectorySyncJobState.FAILED);
      jobRecord.setJobException(ex.toString());
      directorySyncJobRecordRepository.save(jobRecord);
      throw new EntityNotStorableException();
    }
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
        addMissingCollections(newOrganization, biobankId, discoveryService, accessForm);

        List<MolgenisCollection> newBiobankCollections =
            molgenisService.findAllCollectionsByBiobankId(biobankId);

      } else {
        if (!bb.getName().equals(organization.get().getName())) {
          updateOrganizationName(organization.get(), bb);
        }
        List<MolgenisCollection> biobankCollections =
            molgenisService.findAllCollectionsByBiobankId(bb.getId());
        for (MolgenisCollection c : biobankCollections) {
          String collectionId = c.getId();
          Optional<Resource> r = resourceRepository.findBySourceId(collectionId);
          if (r.isEmpty()) {
            log.info("Adding missing collection for the Biobank: " + bb.getId());
            addMissingCollections(organization.get(), bb.getId(), discoveryService, accessForm);
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
      jobRecord.setJobState(DirectorySyncJobState.FAILED);
      jobRecord.setJobException(ex.toString());
      directorySyncJobRecordRepository.save(jobRecord);
      throw new EntityNotStorableException();
    }
  }

  private void addMissingCollections(
      Organization organization, String biobankId, DiscoveryService service, AccessForm form) {
    List<MolgenisCollection> newBiobankCollections =
        molgenisService.findAllCollectionsByBiobankId(biobankId);
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
        jobRecord.setJobState(DirectorySyncJobState.FAILED);
        jobRecord.setJobException(ex.toString());
        directorySyncJobRecordRepository.save(jobRecord);
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
      jobRecord.setJobState(DirectorySyncJobState.FAILED);
      jobRecord.setJobException(ex.toString());
      directorySyncJobRecordRepository.save(jobRecord);
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
      jobRecord.setJobState(DirectorySyncJobState.FAILED);
      jobRecord.setJobException(ex.toString());
      directorySyncJobRecordRepository.save(jobRecord);
      throw new EntityNotStorableException();
    }
  }

  private DirectorySyncJobRecord createJobRecord() {
    DirectorySyncJobRecord directorySyncJobRecord =
        DirectorySyncJobRecord.builder().jobState(DirectorySyncJobState.SUBMITTED).build();
    try {
      directorySyncJobRecordRepository.save(directorySyncJobRecord);
      return directorySyncJobRecord;
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while creating Directory sync job record");
      log.error(ex);
      jobRecord.setJobState(DirectorySyncJobState.FAILED);
      jobRecord.setJobException(ex.toString());
      directorySyncJobRecordRepository.save(jobRecord);
      throw new EntityNotStorableException();
    }
  }
}
