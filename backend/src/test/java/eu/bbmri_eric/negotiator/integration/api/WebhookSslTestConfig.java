package eu.bbmri_eric.negotiator.integration.api;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

/**
 * Test configuration that provides a custom SSL context for webhook integration tests.
 *
 * <p>This configuration creates an SSL context using a shared truststore that contains the
 * certificate used by WireMock's HTTPS server. When injected into the WebhookService, it enables
 * proper SSL/TLS verification during webhook delivery tests against trusted mock endpoints.
 */
@TestConfiguration(proxyBeanMethods = false)
public class WebhookSslTestConfig {
  static final String SSL_STORE_PASSWORD = "changeit";

  /**
   * Creates an SSL context bean configured with WireMock's certificate truststore.
   *
   * <p>This bean is injected into the WebhookService to establish secure HTTPS connections with
   * proper certificate verification when delivering webhooks to mock endpoints during tests.
   *
   * @return an SSL context configured to trust WireMock's test certificates
   * @throws IllegalStateException if the SSL context cannot be built
   */
  @Bean("secureWebhookSslContext")
  SSLContext secureWebhookSslContext() {
    try {
      return sslContextWithWireMockTrustStore();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build SSL context for webhook tests", e);
    }
  }

  /**
   * Loads the truststore containing WireMock's certificate and builds an SSL context from it.
   *
   * <p>The truststore (wiremock-truststore.jks) contains the certificate authority that signed
   * WireMock's HTTPS certificate, allowing the application to verify and trust HTTPS connections to
   * WireMock servers during tests.
   *
   * @return an SSL context initialized with the WireMock truststore
   * @throws Exception if the truststore cannot be loaded or the SSL context cannot be created
   */
  private SSLContext sslContextWithWireMockTrustStore() throws Exception {
    ClassPathResource trustStoreResource = new ClassPathResource("ssl/wiremock-truststore.jks");
    KeyStore trustStore = KeyStore.getInstance("JKS");
    try (InputStream trustStoreStream = trustStoreResource.getInputStream()) {
      trustStore.load(trustStoreStream, SSL_STORE_PASSWORD.toCharArray());
    }
    return SSLContexts.custom().loadTrustMaterial(trustStore, null).build();
  }
}
