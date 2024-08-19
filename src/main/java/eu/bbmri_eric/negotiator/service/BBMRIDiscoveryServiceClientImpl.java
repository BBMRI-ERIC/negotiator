package eu.bbmri_eric.negotiator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.DiscoveryService;
import eu.bbmri_eric.negotiator.database.model.DiscoveryServiceSynchronizationJob;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.DiscoveryServiceRepository;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri_eric.negotiator.governance.organization.Organization;
import eu.bbmri_eric.negotiator.governance.organization.OrganizationRepository;
import eu.bbmri_eric.negotiator.governance.resource.Resource;
import eu.bbmri_eric.negotiator.governance.resource.ResourceRepository;
import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.governance.resource.dto.MolgenisCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.exception.DataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@CommonsLog
@Service(value = "DefaultDiscoveryServiceClient")
public class BBMRIDiscoveryServiceClientImpl implements DiscoveryServiceClient {
  private BBMRIDiscoveryServiceClientImpl bbmriService = null;

  @Autowired private OrganizationRepository organizationRepository;

  @Autowired private AccessFormRepository accessFormRepository;

  @Autowired private DiscoveryServiceRepository discoveryServiceRepository;
  @Autowired private ResourceRepository resourceRepository;

  private DiscoveryServiceSynchronizationJob jobRecord;

  private WebClient webClient;

  public BBMRIDiscoveryServiceClientImpl(@Value("${negotiator.molgenis-url}") String molgenisURL) {
    final int dataBufferSizeLimit = 16 * 1024 * 1024;
    final ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(dataBufferSizeLimit))
            .build();
    this.webClient =
        WebClient.builder().baseUrl(molgenisURL).exchangeStrategies(strategies).build();
  }

  public void syncAllOrganizations() {
    List<MolgenisBiobank> biobanks = findAllBiobanks();
    addMissingOrganizations(biobanks);
  }

  public void syncAllResources() {
    List<MolgenisCollection> collections = findAllCollections();
    addMissingResources(collections);
  }

  public void syncAllNetworks() {}

  private void addMissingOrganizations(List<MolgenisBiobank> directoryBiobanks) {
    for (MolgenisBiobank bb : directoryBiobanks) {
      String biobankId = bb.getId();
      Optional<Organization> organization = organizationRepository.findByExternalId(biobankId);
      if (organization.isEmpty()) {
        addMissingOrganization(bb);
      } else {
        log.debug(String.format("Biobank %s already present, check for updates...", biobankId));
        if (!bb.getName().equals(organization.get().getName())) {
          updateOrganizationName(organization.get(), bb);
        }
      }
    }
  }

  private void addMissingResources(List<MolgenisCollection> directoryCollections) {
    DiscoveryService discoveryService =
        discoveryServiceRepository
            .findById(Long.valueOf("1"))
            .orElseThrow(() -> new EntityNotFoundException("1"));
    AccessForm accessForm =
        accessFormRepository
            .findById(Long.valueOf("1"))
            .orElseThrow(() -> new EntityNotFoundException("1"));
    for (MolgenisCollection coll : directoryCollections) {
      Optional<Resource> resource = resourceRepository.findBySourceId(coll.getId());
      if (resource.isEmpty()) {
        addMissingResource(coll, discoveryService, accessForm);
      } else {
        log.debug(
            String.format("Collection %s already present, check for updates...", coll.getId()));
        if (!Objects.equals(resource.get().getName(), coll.getName())
            || !Objects.equals(resource.get().getDescription(), coll.getDescription())) {
          updateResourceNameAndDescription(resource.get(), coll);
        }
      }
    }
  }

  private void addMissingResource(
      MolgenisCollection collection, DiscoveryService discoveryService, AccessForm accessForm) {
    log.info("Adding collection:" + collection.getId());
    log.info("Biobank external id:" + collection.getBiobank().getId());
    try {
      Optional<Organization> organization =
          organizationRepository.findByExternalId(collection.getBiobank().getId());
      Resource newResource =
          Resource.builder()
              .organization(organization.get())
              .discoveryService(discoveryService)
              .accessForm(accessForm)
              .sourceId(collection.getId())
              .name(collection.getName())
              .description(collection.getDescription())
              .build();

      resourceRepository.save(newResource);
    } catch (DataException | DataIntegrityViolationException ex) {
      log.error("Error while adding missing Collection as a resource, possible withdrawn biobank");
      log.error(ex);
      throw new EntityNotStorableException();
    } catch (NoSuchElementException ex) {
      log.error("Error while adding missing Collection as a resource, possible withdrawn biobank");
    }
  }

  private Organization addMissingOrganization(MolgenisBiobank biobank) {
    log.info("Adding organization: " + biobank.getId());
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

  private void updateOrganizationName(Organization organization, MolgenisBiobank biobank) {
    log.info(String.format("Updating name for existing organization %s", biobank.getId()));
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

  private List<MolgenisBiobank> findAllBiobanks() {
    try {
      String response =
          webClient
              .get()
              .uri("/api/v2/eu_bbmri_eric_biobanks?num=10000&q=withdrawn==false&attrs=id,name")
              .retrieve()
              .bodyToMono(String.class)
              .block();
      JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
      JsonArray items = jsonResponse.getAsJsonArray("items");
      List<MolgenisBiobank> biobanks = new ArrayList<MolgenisBiobank>();

      for (JsonElement e : items) {
        JsonObject jsonBiobank = e.getAsJsonObject();
        MolgenisBiobank biobank =
            new MolgenisBiobank(
                jsonBiobank.get("id").getAsString(),
                jsonBiobank.get("name").getAsString(),
                jsonBiobank.get("_href").getAsString());
        biobanks.add(biobank);
      }
      return biobanks;
    } catch (WebClientResponseException | WebClientRequestException e) {
      log.warn(e.getMessage());
      log.warn("Molgenis is not reachable!");
      return Collections.emptyList();
    }
  }

  private List<MolgenisCollection> findAllCollections() {
    try {
      String response =
          webClient
              .get()
              .uri("/api/v2/eu_bbmri_eric_collections?num=10000&attrs=id,name,description,biobank")
              .retrieve()
              .bodyToMono(String.class)
              .block();
      JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
      JsonArray items = jsonResponse.getAsJsonArray("items");
      List<MolgenisCollection> collections = new ArrayList<MolgenisCollection>();

      for (JsonElement e : items) {
        JsonObject jsonCollection = e.getAsJsonObject();
        MolgenisCollection collection =
            new MolgenisCollection(
                jsonCollection.get("id").getAsString(),
                jsonCollection.get("name").getAsString(),
                jsonCollection.get("description").getAsString(),
                new MolgenisBiobank(
                    jsonCollection.get("biobank").getAsJsonObject().get("id").getAsString(),
                    jsonCollection.get("biobank").getAsJsonObject().get("name").getAsString(),
                    jsonCollection.get("biobank").getAsJsonObject().get("_href").getAsString()));
        collections.add(collection);
      }
      return collections;
    } catch (WebClientResponseException | WebClientRequestException e) {
      log.warn(e.getMessage());
      log.warn("Molgenis is not reachable!");
      return Collections.emptyList();
    }
  }
}
