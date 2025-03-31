package eu.bbmri_eric.negotiator.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "DTO for creating a new webhook.")
public class WebhookCreateDTO {

  @Schema(
      description = "Webhook URL. Must start with http:// or https://",
      example = "https://example.com/webhook")
  @NotNull(message = "URL is required")
  @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
  private String url;

  @Schema(
      description = "Flag indicating if SSL verification is enabled",
      example = "true",
      defaultValue = "true")
  private boolean sslVerification = true;

  @Schema(
      description = "Flag indicating if the webhook is active",
      example = "true",
      defaultValue = "true")
  private boolean active = true;

  public WebhookCreateDTO() {}

  public WebhookCreateDTO(String url) {
    this.url = url;
  }

  public WebhookCreateDTO(String url, boolean sslVerification) {
    this.url = url;
    this.sslVerification = sslVerification;
  }

  public WebhookCreateDTO(String url, boolean sslVerification, boolean active) {
    this.url = url;
    this.sslVerification = sslVerification;
    this.active = active;
  }
}
