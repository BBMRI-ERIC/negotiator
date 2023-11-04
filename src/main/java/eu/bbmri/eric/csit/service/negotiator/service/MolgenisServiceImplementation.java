package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisCollection;
import java.util.Objects;
import java.util.Optional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

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
      return false;
    }
  }

  @Override
  public Optional<MolgenisCollection> findCollectionById(String id) {
    Objects.requireNonNull(id, "Collection Id must not be null!");
    MolgenisCollection molgenisCollection =
        webClient
            .get()
            .uri("/api/v2/collections/" + id)
            .retrieve()
            .bodyToMono(MolgenisCollection.class)
            .block();
    return Optional.of(molgenisCollection);
  }
}
