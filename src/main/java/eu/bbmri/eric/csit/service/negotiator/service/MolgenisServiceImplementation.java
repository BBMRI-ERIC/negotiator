package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisBiobank;
import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisCollection;
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
    } catch (WebClientResponseException e) {
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
    } catch (WebClientResponseException e) {
      return Optional.empty();
    }
  }
}
