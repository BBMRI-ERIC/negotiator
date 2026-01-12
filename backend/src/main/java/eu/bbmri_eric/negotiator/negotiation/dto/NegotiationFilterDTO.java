package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRole;
import eu.bbmri_eric.negotiator.negotiation.NegotiationSortField;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;

/** DTO that defines the possible query filters for Negotiations */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Parameters to filter the negotiations")
public class NegotiationFilterDTO implements FilterDTO {
  @Schema(
      description =
          "The role that the user has in the negotiations. It should be one of AUTHOR or REPRESENTATIVE")
  NegotiationRole role;

  @Schema(description = "List of required statuses")
  List<NegotiationState> status;

  @Schema(description = "List of IDs of Organizations for which Negotiations should be fetched")
  List<Long> organizationId;

  @Schema(
      description =
          "Search term to filter negotiations by title, or display ID (case-insensitive partial match)")
  @Size(max = 255, message = "Search term must not exceed 255 characters")
  String search;

  @Schema(
      description = "The date after which the negotiations were created",
      example = "2024-11-18")
  LocalDate createdAfter;

  @Schema(
      description = "The date before which the negotiations were created",
      example = "2024-11-18")
  LocalDate createdBefore;

  @Schema(description = "The field to use to sort the results", example = "creationDate")
  NegotiationSortField sortBy = NegotiationSortField.creationDate;

  @Schema(description = "The direction of the sorting", example = "DESC")
  Sort.Direction sortOrder = Sort.Direction.DESC;

  @Schema(description = "The page number required", example = "0")
  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  int page = 0;

  @Schema(description = "The size of the pages required", example = "50")
  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  int size = 50;
}
