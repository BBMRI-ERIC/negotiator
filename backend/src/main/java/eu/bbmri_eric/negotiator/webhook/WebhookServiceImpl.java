package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.common.JSONUtils;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import javax.net.ssl.SSLException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookServiceImpl implements WebhookService {
  private static final String REQUEST_TIMEOUT_ERROR_MESSAGE = "Request timeout";
  private static final String SSL_VALIDATION_ERROR_MESSAGE = "SSL certificate validation failed";

  private final WebhookRepository webhookRepository;
  private final ModelMapper modelMapper;
  private final RestTemplate secureRestTemplate;
  private final RestTemplate insecureRestTemplate;

  public WebhookServiceImpl(
      WebhookRepository webhookRepository,
      ModelMapper modelMapper,
      @Qualifier("secureWebhookRestTemplate") RestTemplate secureRestTemplate,
      @Qualifier("insecureWebhookRestTemplate") RestTemplate insecureRestTemplate) {
    this.webhookRepository = webhookRepository;
    this.modelMapper = modelMapper;
    this.secureRestTemplate = secureRestTemplate;
    this.insecureRestTemplate = insecureRestTemplate;
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
    Webhook webhook = getWebhook(webhookId);
    RestTemplate restTemplate =
        webhook.isSslVerification() ? secureRestTemplate : insecureRestTemplate;
    return deliverWebhook(jsonPayload, restTemplate, webhook, null, null);
  }

  @Override
  @Transactional
  public void deliverToActiveWebhooks(String jsonPayload, String eventType, Instant occurredAt) {
    if (!JSONUtils.isJSONValid(jsonPayload)) {
      throw new IllegalArgumentException("Content is not a valid JSON");
    }
    List<Webhook> webhooks = webhookRepository.findByActiveTrue();
    for (Webhook webhook : webhooks) {
      RestTemplate restTemplate =
          webhook.isSslVerification() ? secureRestTemplate : insecureRestTemplate;
      deliverWebhook(jsonPayload, restTemplate, webhook, eventType, occurredAt);
    }
  }

  private @NotNull Webhook getWebhook(Long webhookId) {
    Webhook webhook =
        webhookRepository
            .findById(webhookId)
            .orElseThrow(
                () -> new EntityNotFoundException("Webhook not found with id: " + webhookId));
    if (!webhook.isActive()) {
      throw new IllegalArgumentException("Webhook is not active, therefore cannot deliver");
    }
    return webhook;
  }

  private DeliveryDTO deliverWebhook(
      String jsonPayload,
      RestTemplate restTemplate,
      Webhook webhook,
      String eventType,
      Instant occurredAt) {
    Delivery delivery;
    try {
      HttpEntity<String> request = buildHttpEntity(jsonPayload, eventType, occurredAt);
      int statusCode = postWebhook(restTemplate, webhook, request);
      delivery = new Delivery(jsonPayload, statusCode);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      delivery = new Delivery(jsonPayload, ex.getStatusCode().value(), parseErrorMessage(ex));
    } catch (ResourceAccessException ex) {
      delivery = mapTransportException(jsonPayload, ex);
    } catch (Exception ex) {
      delivery = new Delivery(jsonPayload, null, parseErrorMessage(ex));
    }
    return persistDelivery(webhook, delivery);
  }

  private Delivery mapTransportException(String jsonPayload, ResourceAccessException ex) {
    if (containsCause(ex, ConnectTimeoutException.class)
        || containsCause(ex, SocketTimeoutException.class)) {
      return new Delivery(jsonPayload, null, REQUEST_TIMEOUT_ERROR_MESSAGE);
    }

    if (containsCause(ex, SSLException.class)) {
      return new Delivery(jsonPayload, null, SSL_VALIDATION_ERROR_MESSAGE);
    }
    return new Delivery(jsonPayload, null, parseErrorMessage(ex));
  }

  private DeliveryDTO persistDelivery(Webhook webhook, Delivery delivery) {
    webhook.addDelivery(delivery);
    webhook = webhookRepository.saveAndFlush(webhook);
    return modelMapper.map(webhook.getDeliveries().get(0), DeliveryDTO.class);
  }

  private static int postWebhook(
      RestTemplate restTemplate, Webhook webhook, HttpEntity<String> request) {
    return restTemplate
        .postForEntity(webhook.getUrl(), request, String.class)
        .getStatusCode()
        .value();
  }

  private static @NotNull HttpEntity<String> buildHttpEntity(String jsonPayload, String eventType) {
    return buildHttpEntity(jsonPayload, eventType, null);
  }

  private static @NotNull HttpEntity<String> buildHttpEntity(
      String jsonPayload, String eventType, Instant occurredAt) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (eventType != null && !eventType.isBlank()) {
      headers.add(WebhookHeaders.EVENT_TYPE, eventType);
    }
    if (occurredAt != null) {
      headers.add(WebhookHeaders.OCCURRED_AT, occurredAt.toString());
    }
    return new HttpEntity<>(jsonPayload, headers);
  }

  private String parseErrorMessage(Exception ex) {
    return StringUtils.abbreviate(ex.getMessage(), "...", 255);
  }

  private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeClass) {
    return ExceptionUtils.indexOfType(throwable, causeClass) != -1;
  }
}
