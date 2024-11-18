package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "negotiations", itemRelation = "negotiation")
public class NegotiationDTO {

  @NotNull
  @Schema(
      description = "Unique identifier of the negotiation",
      example = "123e4567-e89b-12d3-a456-426614174000")
  private String id;

  @Schema(description = "Details about the author of the negotiation")
  private UserResponseModel author;

  @NotNull
  @Schema(
      description = "Payload containing additional negotiation data",
      example = "{ \"key\": \"value\" }",
      type = "object")
  private JsonNode payload;

  @NotNull
  @Schema(description = "Current status of the negotiation", example = "PENDING")
  private String status;

  @NotNull
  @Schema(
      description = "Human-readable description of the negotiation",
      example = "Negotiation between parties X and Y")
  private String humanReadable;

  @NotNull
  @Schema(
      description = "Indicates if public posts are enabled for this negotiation",
      example = "true")
  private boolean publicPostsEnabled;

  @NotNull
  @Schema(
      description = "Indicates if private posts are enabled for this negotiation",
      example = "false")
  private boolean privatePostsEnabled;

  @NotNull
  @Schema(
      description = "The timestamp when the negotiation was created",
      example = "2024-11-18T12:00:00")
  private LocalDateTime creationDate;

  @NotNull
  @Schema(
      description = "The timestamp when the negotiation was last modified",
      example = "2024-11-18T15:00:00")
  private LocalDateTime modifiedDate;

  @JsonIgnore
  public String getStatusForResource(String resourceId) {
    return "";
  }
}
