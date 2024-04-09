package eu.bbmri_eric.negotiator.dto.negotiation;

import eu.bbmri_eric.negotiator.api.controller.v3.utils.NegotiationRole;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
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
public class NegotiationFilters {
  NegotiationRole role;

  List<NegotiationState> status;

  LocalDate createdAfter;

  LocalDate createdBefore;
}
