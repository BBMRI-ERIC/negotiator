package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationUpdateDTO {
  @Schema(description = "Payload with the details of the Negotiation")
  private JsonNode payload;

  @Schema(description = "Display id for the negotiation (user-friendly name)", example = "2024-001")
  private String displayId;

  @Schema(
      description = "Subject ID of the user to whom should this Negotiation be transferred to",
      example = "123e4567-e89b-12d3-a456-426614174000@ls-aai.eu")
  private String authorSubjectId;
}
