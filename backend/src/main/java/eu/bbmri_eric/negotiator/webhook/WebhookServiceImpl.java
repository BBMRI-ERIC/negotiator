package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.common.JSONUtils;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.apachecommons.CommonsLog;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@CommonsLog
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
  @Transactional
  public WebhookResponseDTO getWebhookById(Long id) {
    Webhook webhook =
        webhookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(webhook, WebhookResponseDTO.class);
  }

  @Override
  @Transactional
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
    if (!JSONUtils.isJSONValid(jsonPayload)) {
      throw new IllegalArgumentException("Content is not a valid JSON");
    }
    System.out.println(jsonPayload);
    Webhook webhook =
        webhookRepository
            .findById(webhookId)
            .orElseThrow(
                () -> new EntityNotFoundException("Webhook not found with id: " + webhookId));
    if (!webhook.isActive()) {
      throw new IllegalArgumentException("Webhook is not active, therefore cannot deliver");
    }
    RestTemplate restTemplate =
        webhook.isSslVerification() ? new RestTemplate() : createNoSSLRestTemplate();
    return deliverWebhook(jsonPayload, restTemplate, webhook);
  }

  private DeliveryDTO deliverWebhook(
      String jsonPayload, RestTemplate restTemplate, Webhook webhook) {
    Delivery delivery;
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      String payload = JSONUtils.toJSON(jsonPayload);
      HttpEntity<String> request = new HttpEntity<>(payload, headers);
      ResponseEntity<String> response =
          restTemplate.postForEntity(webhook.getUrl(), request, String.class);
      int statusCode = response.getStatusCode().value();
      delivery = new Delivery(jsonPayload, statusCode);
    } catch (org.springframework.web.client.HttpClientErrorException
        | org.springframework.web.client.HttpServerErrorException ex) {
      delivery =
          new Delivery(jsonPayload, ex.getStatusCode().value(), parseErrorMessage(ex.getMessage()));
    } catch (Exception ex) {
      delivery = new Delivery(jsonPayload, 500, parseErrorMessage(ex.getMessage()));
    }
    webhook.addDelivery(delivery);
    webhook = webhookRepository.saveAndFlush(webhook);
    return modelMapper.map(webhook.getDeliveries().get(0), DeliveryDTO.class);
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

              public void checkClientTrusted(X509Certificate[] certs, String authType) {
                // Empty implementation
              }

              public void checkServerTrusted(X509Certificate[] certs, String authType) {
                // Empty implementation
              }
            }
          },
          new SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
      HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
      return new RestTemplate();
    } catch (Exception e) {
      throw new InternalError();
    }
  }

  private String parseErrorMessage(String errorMessage) {
    if (errorMessage != null && errorMessage.length() > 255) {
      return errorMessage.substring(0, 255);
    } else {
      return errorMessage;
    }
  }
}
