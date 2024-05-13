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
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@CommonsLog
public class MolgenisDirectorySyncService {

  @Value("${negotiator.molgenis-url}")
  private String molgenisURL;

  private MolgenisService molgenisService = null;

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
    log.info("Started sync Directory recources at: " + new Date());
    DirectorySyncJobRecord directorySyncJobRecord = createJobRecord();
    log.info("Fetching all organizations... " + new Date());
    List<MolgenisBiobank> biobanks = molgenisService.findAllBiobanks();
    log.info("Adding missing organizations... " + new Date());
    addMissingOrganizationsAndResources(biobanks);
    log.info("Job successfully completed");
    directorySyncJobRecord.setJobState(DirectorySyncJobState.COMPLETED);
    directorySyncJobRecordRepository.save(directorySyncJobRecord);
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
        // If the organization is present, check if the metadata hasn't changed and update it;
        // same for the Collections
        // additionally, add all the missing collections
        if (!bb.getName().equals(organization.get().getName())) {
          updateOrganizationName(organization.get(), bb);
        }
        // get collections at the moment present in the Negotator
        // List<Resource> resources =
        // resourceRepository.findAllByOrganizationId(organization.get().getId());
        // for (Resource r : resources){
        // check if the resource is not present, and add it

        // }
        // Collections (resources) retrieved from directory
        List<MolgenisCollection> biobankCollections =
            molgenisService.findAllCollectionsByBiobankId(bb.getId());
        for (MolgenisCollection c : biobankCollections) {
          String collectionId = c.getId();
          Optional<Resource> r = resourceRepository.findBySourceId(collectionId);
          if (r.isEmpty()) {
            // add the missing collection to the Resources, for that biobank
            log.info("Adding missing collection for the Biobank: " + bb.getId());
            addMissingCollections(organization.get(), bb.getId(), discoveryService, accessForm);
          } else {
            // The Collection is already present. Check name and description information and
            // eventually update it
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
    organizationRepository.save(newOrganization);
    return newOrganization;
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
      resourceRepository.save(newResource);
    }
  }

  private void updateOrganizationName(Organization organization, MolgenisBiobank biobank) {
    log.info(String.format("Updating name for existing organization {0}", biobank.getId()));
    organization.setName(biobank.getName());
    organizationRepository.save(organization);
  }

  private void updateResourceNameAndDescription(Resource resource, MolgenisCollection collection) {
    resource.setName(collection.getName());
    resource.setDescription(collection.getDescription());
    resourceRepository.save(resource);
  }

  private DirectorySyncJobRecord createJobRecord() {
    DirectorySyncJobRecord directorySyncJobRecord =
        DirectorySyncJobRecord.builder().jobState(DirectorySyncJobState.SUBMITTED).build();
    directorySyncJobRecordRepository.save(directorySyncJobRecord);
    return directorySyncJobRecord;
  }
}
