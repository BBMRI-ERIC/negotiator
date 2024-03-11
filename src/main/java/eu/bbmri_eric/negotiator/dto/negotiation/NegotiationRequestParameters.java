package eu.bbmri_eric.negotiator.dto.negotiation;

import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationRole;
import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationSortOrder;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO that defines the possible query filters for Negotiations */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegotiationRequestParameters {
  NegotiationRole role;

  List<NegotiationState> state;

  LocalDate createdAfter;

  LocalDate createdBefore;

  @NotBlank String sortBy = "creationDate";

  NegotiationSortOrder sortOrder = NegotiationSortOrder.DESC;

  @Min(0)
  int page = 0;

  @Min(1)
  int size = 50;
}
