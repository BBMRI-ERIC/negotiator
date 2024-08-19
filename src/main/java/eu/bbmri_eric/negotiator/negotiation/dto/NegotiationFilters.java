package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.negotiation.NegotiationRole;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
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
