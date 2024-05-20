package eu.bbmri_eric.negotiator.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.bbmri_eric.negotiator.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.dto.MolgenisCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@CommonsLog
public class MolgenisServiceImplementation implements MolgenisService {

  private final WebClient webClient;

  public MolgenisServiceImplementation(WebClient webClient) {
    Objects.requireNonNull(webClient, "WebClient must not be null!");
    this.webClient = webClient;
  }

  @Override
  public boolean isReachable() {
    try {
      return Objects.requireNonNull(
              webClient
                  .get()
                  .exchangeToMono(clientResponse -> Mono.just(clientResponse.statusCode()))
                  .block())
          .is2xxSuccessful();
    } catch (WebClientRequestException e) {
      log.warn(e.getMessage());
      log.warn("Molgenis is not reachable!");
      return false;
    }
  }

  @Override
  public Optional<MolgenisCollection> findCollectionById(String id) {
    Objects.requireNonNull(id, "Collection Id must not be null!");
    try {
      return Optional.ofNullable(
          webClient
              .get()
              .uri("/api/v2/eu_bbmri_eric_collections/" + id)
              .retrieve()
              .bodyToMono(MolgenisCollection.class)
              .block());
    } catch (WebClientResponseException | WebClientRequestException e) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<MolgenisBiobank> findBiobankById(String id) {
    Objects.requireNonNull(id, "Biobank Id must not be null!");
    try {
      return Optional.ofNullable(
          webClient
              .get()
              .uri("/api/v2/eu_bbmri_eric_biobanks/" + id)
              .retrieve()
              .bodyToMono(MolgenisBiobank.class)
              .block());
    } catch (WebClientResponseException | WebClientRequestException e) {
      log.warn("Molgenis is not reachable!");
      return Optional.empty();
    }
  }

  @Override
  public List<MolgenisBiobank> findAllBiobanks() {
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

  @Override
  public List<MolgenisCollection> findAllCollectionsByBiobankId(MolgenisBiobank biobank) {
    try {
      String response =
          webClient
              .get()
              .uri(
                  String.format(
                      "/api/v2/eu_bbmri_eric_collections?q=biobank==%s&num=10000&attrs=id,name,description",
                      biobank.getId()))
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
                biobank);
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
