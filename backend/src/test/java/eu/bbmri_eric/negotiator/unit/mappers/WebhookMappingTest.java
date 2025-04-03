package eu.bbmri_eric.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.bbmri_eric.negotiator.webhook.Webhook;
import eu.bbmri_eric.negotiator.webhook.WebhookCreateDTO;
import eu.bbmri_eric.negotiator.webhook.WebhookResponseDTO;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

public class WebhookMappingTest {

  private final ModelMapper modelMapper = new ModelMapper();

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
}
