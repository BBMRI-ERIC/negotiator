package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookServiceImpl implements WebhookService {
  private final WebhookRepository webhookRepository;
  private final ModelMapper modelMapper;

  public WebhookServiceImpl(WebhookRepository webhookRepository, ModelMapper modelMapper) {
    this.webhookRepository = webhookRepository;
    this.modelMapper = modelMapper;
  }

  @Override
  public WebhookResponseDTO createWebhook(WebhookCreateDTO dto) {
    Webhook webhook = modelMapper.map(dto, Webhook.class);
    Webhook savedWebhook = webhookRepository.save(webhook);
    return modelMapper.map(savedWebhook, WebhookResponseDTO.class);
  }

  @Override
  public WebhookResponseDTO getWebhookById(Long id) {
    Webhook webhook =
        webhookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(webhook, WebhookResponseDTO.class);
  }

  @Override
  public List<WebhookResponseDTO> getAllWebhooks() {
    List<Webhook> webhooks = webhookRepository.findAll();
    return webhooks.stream()
        .map(webhook -> modelMapper.map(webhook, WebhookResponseDTO.class))
        .toList();
  }

  @Override
  public WebhookResponseDTO updateWebhook(Long id, WebhookCreateDTO dto) {
    Webhook existingWebhook =
        webhookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    modelMapper.map(dto, existingWebhook);
    Webhook updatedWebhook = webhookRepository.save(existingWebhook);
    return modelMapper.map(updatedWebhook, WebhookResponseDTO.class);
  }

  @Override
  public void deleteWebhook(Long id) {
    if (!webhookRepository.existsById(id)) {
      throw new EntityNotFoundException(id);
    }
    webhookRepository.deleteById(id);
  }

  @Override
  @Transactional
  public DeliveryDTO deliver(String jsonPayload, Long webhookId) {
    Webhook webhook =
        webhookRepository
            .findById(webhookId)
            .orElseThrow(
                () -> new EntityNotFoundException("Webhook not found with id: " + webhookId));
    RestTemplate restTemplate =
        webhook.isSslVerification() ? new RestTemplate() : createNoSSLRestTemplate();
    return tryToDeliver(jsonPayload, restTemplate, webhook);
  }

  private DeliveryDTO tryToDeliver(String jsonPayload, RestTemplate restTemplate, Webhook webhook) {
    Delivery delivery;
    try {
      ResponseEntity<String> response =
          restTemplate.postForEntity(webhook.getUrl(), jsonPayload, String.class);
      int statusCode = response.getStatusCode().value();
      boolean success = response.getStatusCode() == HttpStatus.OK;
      String errorMessage = success ? null : response.getBody();
      delivery = new Delivery(jsonPayload, statusCode, errorMessage);
    } catch (Exception ex) {
      delivery = new Delivery(jsonPayload, 500, ex.getMessage());
    }
    webhook.addDelivery(delivery);
    return modelMapper.map(delivery, DeliveryDTO.class);
  }

  private RestTemplate createNoSSLRestTemplate() {
    try {
      SSLContext sslContext = SSLContext.getInstance("TLS");
      sslContext.init(
          null,
          new TrustManager[] {
            new X509TrustManager() {
              public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
              }

              public void checkClientTrusted(X509Certificate[] certs, String authType) {}

              public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
          },
          new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
      return new RestTemplate();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
