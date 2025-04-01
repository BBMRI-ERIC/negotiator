package eu.bbmri_eric.negotiator.webhook;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.json.JSONObject;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v3/webhooks", produces = "application/json", consumes = "application/json")
@Tag(name = "Webhook API", description = "Operations related to webhooks")
@SecurityRequirement(name = "security_auth")
public class WebhookController {

  private final WebhookService webhookService;
  private final WebhookModelAssembler assembler;

  public WebhookController(WebhookService webhookService, WebhookModelAssembler assembler) {
    this.webhookService = webhookService;
    this.assembler = assembler;
  }

  @Operation(summary = "Get all webhooks", description = "Fetches all registered webhooks")
  @GetMapping
  public CollectionModel<EntityModel<WebhookResponseDTO>> getAllWebhooks() {
    List<WebhookResponseDTO> dtos = webhookService.getAllWebhooks();
    return assembler.toCollectionModel(dtos);
  }

  @Operation(summary = "Get a webhook by id", description = "Fetches a single webhook by its id")
  @GetMapping("/{id}")
  public EntityModel<WebhookResponseDTO> getWebhookById(@PathVariable Long id) {
    WebhookResponseDTO dto = webhookService.getWebhookById(id);
    return assembler.toModel(dto);
  }

  @Operation(
      summary = "Create a webhook",
      description = "Creates a new webhook with the provided data")
  @PostMapping
  public EntityModel<WebhookResponseDTO> createWebhook(@Valid @RequestBody WebhookCreateDTO dto) {
    WebhookResponseDTO created = webhookService.createWebhook(dto);
    return assembler.toModel(created);
  }

  @Operation(
      summary = "Update a webhook",
      description = "Updates an existing webhook using the provided data")
  @PatchMapping("/{id}")
  public EntityModel<WebhookResponseDTO> updateWebhook(
      @PathVariable Long id, @Valid @RequestBody WebhookCreateDTO dto) {
    WebhookResponseDTO updated = webhookService.updateWebhook(id, dto);
    return assembler.toModel(updated);
  }

  @Operation(
      summary = "Delete a webhook",
      description = "Deletes the webhook with the specified id")
  @DeleteMapping("/{id}")
  public void deleteWebhook(@PathVariable Long id) {
    webhookService.deleteWebhook(id);
  }

  @Operation(
      summary = "Deliver a message to a webhook",
      description = "Adds a new delivery to the specified webhook and returns the response")
  @PostMapping(value = "/{id}/deliveries")
  public EntityModel<DeliveryDTO> addDelivery(
      @PathVariable Long id,
      @Valid
          @RequestBody
          @Schema(description = "Content to deliver", example = "{\"test\":\"yes\"}")
          JSONObject content) {
    DeliveryDTO dto = webhookService.deliver(content.toString(), id);
    return EntityModel.of(dto);
  }

}
