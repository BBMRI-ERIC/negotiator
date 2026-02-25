package eu.bbmri_eric.negotiator.webhook;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration(proxyBeanMethods = false)
class WebhookRestTemplateConfig {
  private final RestTemplateBuilder restTemplateBuilder;
  private final WebhookTimeoutProperties webhookHttpClientProperties;

  WebhookRestTemplateConfig(
      RestTemplateBuilder restTemplateBuilder,
      WebhookTimeoutProperties webhookHttpClientProperties) {
    this.restTemplateBuilder = restTemplateBuilder;
    this.webhookHttpClientProperties = webhookHttpClientProperties;
  }

  @Bean
  @ConditionalOnMissingBean(name = "secureWebhookRestTemplate")
  RestTemplate secureWebhookRestTemplate(
      @Qualifier("secureWebhookHttpClient") CloseableHttpClient secureWebhookHttpClient) {
    return restTemplateBuilder
        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(secureWebhookHttpClient))
        .build();
  }

  @Bean(destroyMethod = "close")
  CloseableHttpClient secureWebhookHttpClient(
      @Qualifier("secureWebhookSslContext") SSLContext secureWebhookSslContext) {
    return HttpClients.custom()
        .setDefaultRequestConfig(requestConfig())
        .setConnectionManager(
            connectionManager(secureWebhookSslContext, new DefaultHostnameVerifier()))
        .build();
  }

  @Bean
  @ConditionalOnMissingBean(name = "secureWebhookSslContext")
  SSLContext secureWebhookSslContext() {
    try {
      return SSLContexts.createSystemDefault();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to initialize default SSL context", e);
    }
  }

  @Bean(destroyMethod = "close")
  CloseableHttpClient insecureWebhookHttpClient() {
    try {
      SSLContext insecureWebhookSslContext =
          SSLContexts.custom().loadTrustMaterial(TrustAllStrategy.INSTANCE).build();
      return HttpClients.custom()
          .setDefaultRequestConfig(requestConfig())
          .setConnectionManager(
              connectionManager(insecureWebhookSslContext, NoopHostnameVerifier.INSTANCE))
          .build();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to create insecure webhook HTTP client", e);
    }
  }

  @Bean
  RestTemplate insecureWebhookRestTemplate(
      @Qualifier("insecureWebhookHttpClient") CloseableHttpClient insecureWebhookHttpClient) {
    return restTemplateBuilder
        .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(insecureWebhookHttpClient))
        .build();
  }

  private RequestConfig requestConfig() {
    return RequestConfig.custom()
        .setResponseTimeout(Timeout.of(webhookHttpClientProperties.getRead()))
        .build();
  }

  private ConnectionConfig connectionConfig() {
    return ConnectionConfig.custom()
        .setConnectTimeout(Timeout.of(webhookHttpClientProperties.getConnect()))
        .build();
  }

  private PoolingHttpClientConnectionManager connectionManager(
      SSLContext sslContext, HostnameVerifier hostnameVerifier) {
    DefaultClientTlsStrategy tlsStrategy =
        new DefaultClientTlsStrategy(sslContext, hostnameVerifier);
    return PoolingHttpClientConnectionManagerBuilder.create()
        .setDefaultConnectionConfig(connectionConfig())
        .setTlsSocketStrategy(tlsStrategy)
        .build();
  }
}
