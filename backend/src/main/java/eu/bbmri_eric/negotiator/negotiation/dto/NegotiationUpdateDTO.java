package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NegotiationUpdateDTO {
    @Schema(description = "Human-readable display ID of the negotiation", example = "1001")
    @Size(max = 20, message = "Display ID must not exceed 20 characters")
    private String displayId;

    @Schema(description = "Payload with the details of the Negotiation")
    private JsonNode payload;

    @Schema(
            description = "Subject ID of the user to whom should this Negotiation be transferred to",
            example = "123e4567-e89b-12d3-a456-426614174000@ls-aai.eu")
    private String authorSubjectId;
}
