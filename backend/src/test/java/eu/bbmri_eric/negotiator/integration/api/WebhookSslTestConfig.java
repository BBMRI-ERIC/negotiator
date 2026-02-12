package eu.bbmri_eric.negotiator.integration.api;

import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.hc.core5.ssl.SSLContexts;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;

@TestConfiguration(proxyBeanMethods = false)
public class WebhookSslTestConfig {
  static final String SSL_STORE_PASSWORD = "changeit";

  @Bean("secureWebhookSslContext")
  SSLContext secureWebhookSslContext() {
    try {
      return sslContextWithWireMockTrustStore();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to build SSL context for webhook tests", e);
    }
  }

  private SSLContext sslContextWithWireMockTrustStore() throws Exception {
    ClassPathResource trustStoreResource = new ClassPathResource("ssl/wiremock-truststore.jks");
    KeyStore trustStore = KeyStore.getInstance("JKS");
    try (InputStream trustStoreStream = trustStoreResource.getInputStream()) {
      trustStore.load(trustStoreStream, SSL_STORE_PASSWORD.toCharArray());
    }
    return SSLContexts.custom().loadTrustMaterial(trustStore, null).build();
  }
}
