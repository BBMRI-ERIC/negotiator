package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
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

  @PostConstruct
  public void init() {
    molgenisService = new MolgenisServiceImplementation(WebClient.create(molgenisURL));
  }

  public void syncDirectoryResources() {
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
        log.info("Adding organization:" + biobankId);
        // The biobank is not present as an organization in the Negotiator; add it
        Organization newOrganization =
            Organization.builder().externalId(biobankId).name(bb.getName()).build();
        organizationRepository.save(newOrganization);
        // add all the collections related to that organization in the resource table.
        // we suppose to add for all of them a default access form id (and also discovery service)

        List<MolgenisCollection> newBiobankCollections =
            molgenisService.findAllCollectionsByBiobankId(biobankId);
        for (MolgenisCollection collection : newBiobankCollections) {
          log.info("Adding collection:" + collection.getId());
          Resource newResource =
              Resource.builder()
                  .organization(newOrganization)
                  .discoveryService(discoveryService)
                  .accessForm(accessForm)
                  .sourceId(collection.getId())
                  .name(collection.getName())
                  .description(collection.getDescription())
                  .build();
          resourceRepository.save(newResource);
        }
      } else {
        // If the organization is present, check if the metadata hasn't changed and update it;
        // same for the Collections
        // additionally, add all the missing collections
        if (!bb.getName().equals(organization.get().getName())) {
          log.info(String.format("Updating name for existing organization {0}", bb.getId()));
          organization.get().setName(bb.getName());
          organizationRepository.save(organization.get());
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
            Resource newResource =
                Resource.builder()
                    .sourceId(collectionId)
                    .name(c.getName())
                    .description(c.getDescription())
                    .discoveryService(discoveryService)
                    .accessForm(accessForm)
                    .organization(organization.get())
                    .build();
            resourceRepository.save(newResource);
          }
        }
      }
    }
  }
}
