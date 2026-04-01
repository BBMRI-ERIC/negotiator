package eu.bbmri_eric.negotiator.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO representing a webhook response.")
public class WebhookResponseDTO {

  @Schema(description = "Unique identifier of the webhook", example = "1")
  private Long id;

  @Schema(description = "Webhook URL", example = "https://example.com/webhook")
  private String url;

  @Schema(description = "Flag indicating if SSL verification is enabled", example = "true")
  private boolean sslVerification;

  @Schema(description = "Flag indicating if the webhook is active", example = "true")
  private boolean active;

  @Schema(
      description = "Optional identifier of the associated secret",
      example = "e3429e49-9237-46b0-a122-d11657379f5a")
  private String secretId;

  private List<DeliveryDTO> deliveries;

  public WebhookResponseDTO() {}

  public WebhookResponseDTO(Long id, String url, boolean sslVerification, boolean active) {
    this.id = id;
    this.url = url;
    this.sslVerification = sslVerification;
    this.active = active;
  }
}
