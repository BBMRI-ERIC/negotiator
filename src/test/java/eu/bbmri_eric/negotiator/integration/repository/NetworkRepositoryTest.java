package eu.bbmri_eric.negotiator.integration.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.NegotiatorApplication;
import eu.bbmri_eric.negotiator.database.model.Network;
import eu.bbmri_eric.negotiator.database.repository.NetworkRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = NegotiatorApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"spring.sql.init.mode=never"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NetworkRepositoryTest {

  @Autowired private NetworkRepository networkRepository;

  @Test
  void findByExternalIdReturnsNetworkWhenIdExists() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.setExternalId("known-id");
    networkRepository.save(network);

    Optional<Network> foundNetwork = networkRepository.findByExternalId("known-id");

    assertThat(foundNetwork).isPresent();
  }

  @Test
  void findByExternalIdReturnsEmptyWhenIdDoesNotExist() {
    Optional<Network> foundNetwork = networkRepository.findByExternalId("unknown-id");

    assertThat(foundNetwork).isEmpty();
  }

  @Test
  void existsByExternalIdReturnsTrueWhenIdExists() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.setExternalId("known-id");
    networkRepository.save(network);

    boolean exists = networkRepository.existsByExternalId("known-id");

    assertTrue(exists);
  }

  @Test
  void existsByExternalIdReturnsFalseWhenIdDoesNotExist() {
    boolean exists = networkRepository.existsByExternalId("unknown-id");

    assertFalse(exists);
  }

  @Test
  void existsByUriReturnsTrueWhenUriExists() {
    Network network = Network.builder().uri("http://example.com").externalId("validId").build();
    network.setUri("known-uri");
    networkRepository.save(network);

    boolean exists = networkRepository.existsByUri("known-uri");

    assertTrue(exists);
  }

  @Test
  void existsByUriReturnsFalseWhenUriDoesNotExist() {
    boolean exists = networkRepository.existsByUri("unknown-uri");

    assertFalse(exists);
  }
}
