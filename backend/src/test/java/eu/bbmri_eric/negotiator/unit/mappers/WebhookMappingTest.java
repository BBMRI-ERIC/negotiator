package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookCreateDTO;
import eu.bbmri_eric.negotiator.webhook.WebhookModelMapper;
import eu.bbmri_eric.negotiator.webhook.WebhookResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(
    classes = {WebhookMappingTest.ModelMapperTestConfig.class, WebhookModelMapper.class})
public class WebhookMappingTest {

  @Autowired private ModelMapper modelMapper;

  @Configuration
  static class ModelMapperTestConfig {
    @Bean
    ModelMapper modelMapper() {
      return new ModelMapper();
    }
  }

  @Test
  void testMapping_FromCreateDtoToEntity_ok() {
    WebhookCreateDTO createDto = new WebhookCreateDTO("https://example.com/webhook", true, true);
    Webhook webhook = modelMapper.map(createDto, Webhook.class);
    assertEquals(createDto.getUrl(), webhook.getUrl());
    assertEquals(createDto.isSslVerification(), webhook.isSslVerification());
    assertEquals(createDto.isActive(), webhook.isActive());
  }

  @Test
  void testMapping_FromEntityToResponseDto_ok() {
    Webhook webhook = new Webhook("https://example.com/webhook", true, true);
    webhook.setId(1L);
    WebhookResponseDTO responseDto = modelMapper.map(webhook, WebhookResponseDTO.class);
    assertEquals(webhook.getId(), responseDto.getId());
    assertEquals(webhook.getUrl(), responseDto.getUrl());
    assertEquals(webhook.isSslVerification(), responseDto.isSslVerification());
    assertEquals(webhook.isActive(), responseDto.isActive());
  }

  @Test
  void testMapping_FromCreateDtoToExistingEntity_doesNotMapSecretIdToId() {
    Webhook existingWebhook = new Webhook("https://example.com/webhook", true, true);
    existingWebhook.setId(42L);
    existingWebhook.setSecretId("existing-secret-id");

    WebhookCreateDTO updateDto =
        new WebhookCreateDTO("https://example.com/webhook-updated", true, true);
    updateDto.setSecret(JsonNullable.of("1234567890abcdef"));

    modelMapper.map(updateDto, existingWebhook);

    assertEquals(42L, existingWebhook.getId());
    assertEquals("existing-secret-id", existingWebhook.getSecretId());
  }
}
