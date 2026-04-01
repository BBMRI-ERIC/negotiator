package eu.bbmri_eric.negotiator.webhook;

import eu.bbmri_eric.negotiator.common.JSONUtils;
import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.webhook.event.WebhookEventType;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import javax.net.ssl.SSLException;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.modelmapper.ModelMapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookServiceImpl implements WebhookService {
  private static final String REQUEST_TIMEOUT_ERROR_MESSAGE = "Request timeout";
  private static final String SSL_VALIDATION_ERROR_MESSAGE = "SSL certificate validation failed";

  private final WebhookRepository webhookRepository;
  private final DeliveryRepository deliveryRepository;
  private final ModelMapper modelMapper;
  private final WebhookDeliveryPersister webhookDeliveryPersister;
  private final WebhookSecretService webhookSecretService;
  private final RestTemplate secureRestTemplate;
  private final RestTemplate insecureRestTemplate;

  public WebhookServiceImpl(
      WebhookRepository webhookRepository,
      DeliveryRepository deliveryRepository,
      ModelMapper modelMapper,
      WebhookDeliveryPersister webhookDeliveryPersister,
      WebhookSecretService webhookSecretService,
      @Qualifier("secureWebhookRestTemplate") RestTemplate secureRestTemplate,
      @Qualifier("insecureWebhookRestTemplate") RestTemplate insecureRestTemplate) {
    this.webhookRepository = webhookRepository;
    this.deliveryRepository = deliveryRepository;
    this.modelMapper = modelMapper;
    this.webhookDeliveryPersister = webhookDeliveryPersister;
    this.webhookSecretService = webhookSecretService;
    this.secureRestTemplate = secureRestTemplate;
    this.insecureRestTemplate = insecureRestTemplate;
  }

  @Override
  @Transactional
  public WebhookResponseDTO createWebhook(WebhookCreateDTO dto) {
    Webhook webhook = modelMapper.map(dto, Webhook.class);
    applySecretTransition(webhook, dto.getSecret());
    Webhook savedWebhook = webhookRepository.save(webhook);
    return modelMapper.map(savedWebhook, WebhookResponseDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public WebhookResponseDTO getWebhookById(Long id) {
    Webhook webhook =
        webhookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(webhook, WebhookResponseDTO.class);
  }

  @Override
  @Transactional(readOnly = true)
  public List<WebhookResponseDTO> getAllWebhooks() {
    List<Webhook> webhooks = webhookRepository.findAll();
    return webhooks.stream()
        .map(webhook -> modelMapper.map(webhook, WebhookResponseDTO.class))
        .toList();
  }

  @Override
  @Transactional
  public WebhookResponseDTO updateWebhook(Long id, WebhookCreateDTO dto) {
    Webhook existingWebhook =
        webhookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    modelMapper.map(dto, existingWebhook);
    applySecretTransition(existingWebhook, dto.getSecret());
    Webhook updatedWebhook = webhookRepository.save(existingWebhook);
    return modelMapper.map(updatedWebhook, WebhookResponseDTO.class);
  }

  @Override
  @Transactional
  public void deleteWebhook(Long id) {
    Webhook webhook =
        webhookRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    deleteAssociatedSecret(webhook);
    webhookRepository.deleteById(id);
  }

  @Override
  public DeliveryDTO deliver(String jsonPayload, WebhookEventType eventType, Long webhookId) {
    return deliver(jsonPayload, eventType, webhookId, Instant.now());
  }

  @Override
  @Transactional
  public DeliveryDTO redeliver(Long webhookId, String deliveryId) {
    Webhook webhook = getWebhook(webhookId);
    Delivery sourceDelivery =
        deliveryRepository
            .findByIdAndWebhookId(deliveryId, webhookId)
            .orElseThrow(() -> new EntityNotFoundException(deliveryId));

    RestTemplate restTemplate =
        webhook.isSslVerification() ? secureRestTemplate : insecureRestTemplate;

    Instant timestamp = Instant.from(sourceDelivery.getAt().atZone(ZoneId.systemDefault()));

    String rootDeliveryId =
        sourceDelivery.getRedeliveryOfDeliveryId() == null
            ? deliveryId
            : sourceDelivery.getRedeliveryOfDeliveryId();

    return deliverWebhook(
        sourceDelivery.getContent(),
        restTemplate,
        webhook,
        sourceDelivery.getEventType(),
        timestamp,
        rootDeliveryId);
  }

  @Override
  public DeliveryDTO deliver(
      String jsonPayload, WebhookEventType eventType, Long webhookId, Instant occurredAt) {
    if (!JSONUtils.isJSONValid(jsonPayload)) {
      throw new IllegalArgumentException("Content is not a valid JSON");
    }
    Webhook webhook = getWebhook(webhookId);
    RestTemplate restTemplate =
        webhook.isSslVerification() ? secureRestTemplate : insecureRestTemplate;
    return deliverWebhook(jsonPayload, restTemplate, webhook, eventType, occurredAt, null);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Long> getActiveWebhookIds() {
    return webhookRepository.findByActiveTrue().stream().map(Webhook::getId).toList();
  }

  private void applySecretTransition(Webhook webhook, JsonNullable<String> secretPatch) {
    if (secretPatch == null || !secretPatch.isPresent()) {
      return;
    }
    String plainTextSecret = secretPatch.orElse(null);
    if (plainTextSecret == null) {
      deleteAssociatedSecret(webhook);
      return;
    }

    if (plainTextSecret.isEmpty()) {
      return;
    }

    webhookSecretService.validateSecret(plainTextSecret);
    replaceAssociatedSecret(webhook, plainTextSecret);
  }

  private void replaceAssociatedSecret(Webhook webhook, String plainTextSecret) {
    deleteAssociatedSecret(webhook);
    DecryptedWebhookSecret createdSecret = webhookSecretService.createSecret(plainTextSecret);
    webhook.setSecretId(createdSecret.id());
  }

  private void deleteAssociatedSecret(Webhook webhook) {
    if (webhook.getSecretId() == null) {
      return;
    }
    String currentSecretId = webhook.getSecretId();
    webhook.setSecretId(null);
    webhookSecretService.deleteSecret(currentSecretId);
  }

  private @NonNull Webhook getWebhook(Long webhookId) {
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
      WebhookEventType eventType,
      Instant occurredAt,
      String redeliveryOfDeliveryId) {
    Delivery delivery;
    try {
      HttpEntity<String> request = buildHttpEntity(jsonPayload, eventType, occurredAt);
      int statusCode = postWebhook(restTemplate, webhook, request);
      delivery = new Delivery(jsonPayload, statusCode, eventType);
    } catch (HttpClientErrorException | HttpServerErrorException ex) {
      delivery =
          new Delivery(jsonPayload, ex.getStatusCode().value(), parseErrorMessage(ex), eventType);
    } catch (ResourceAccessException ex) {
      delivery = mapTransportException(jsonPayload, ex, eventType);
    } catch (Exception ex) {
      delivery = new Delivery(jsonPayload, null, parseErrorMessage(ex), eventType);
    }
    delivery.setRedeliveryOfDeliveryId(redeliveryOfDeliveryId);
    return webhookDeliveryPersister.persist(webhook.getId(), delivery);
  }

  private Delivery mapTransportException(
      String jsonPayload, ResourceAccessException ex, WebhookEventType eventType) {
    if (containsCause(ex, ConnectTimeoutException.class)
        || containsCause(ex, SocketTimeoutException.class)) {
      return new Delivery(jsonPayload, null, REQUEST_TIMEOUT_ERROR_MESSAGE, eventType);
    }

    if (containsCause(ex, SSLException.class)) {
      return new Delivery(jsonPayload, null, SSL_VALIDATION_ERROR_MESSAGE, eventType);
    }
    return new Delivery(jsonPayload, null, parseErrorMessage(ex), eventType);
  }

  private static int postWebhook(
      RestTemplate restTemplate, Webhook webhook, HttpEntity<String> request) {
    return restTemplate
        .postForEntity(webhook.getUrl(), request, String.class)
        .getStatusCode()
        .value();
  }

  private static @NonNull HttpEntity<String> buildHttpEntity(
      String jsonPayload, @NonNull WebhookEventType eventType, @NonNull Instant occurredAt) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add(WebhookHeaders.EVENT_TYPE, eventType.value());
    headers.add(WebhookHeaders.TIMESTAMP, occurredAt.toString());

    return new HttpEntity<>(jsonPayload, headers);
  }

  private String parseErrorMessage(Exception ex) {
    return StringUtils.abbreviate(ex.getMessage(), "...", 255);
  }

  private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeClass) {
    return ExceptionUtils.indexOfType(throwable, causeClass) != -1;
  }
}
