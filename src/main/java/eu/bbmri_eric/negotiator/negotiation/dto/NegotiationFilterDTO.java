package eu.bbmri_eric.negotiator.negotiation.dto;

import eu.bbmri_eric.negotiator.common.FilterDTO;
import eu.bbmri_eric.negotiator.negotiation.NegotiationRole;
import eu.bbmri_eric.negotiator.negotiation.NegotiationSortField;
import eu.bbmri_eric.negotiator.negotiation.state_machine.negotiation.NegotiationState;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

/** DTO that defines the possible query filters for Negotiations */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegotiationFilterDTO implements FilterDTO {
  NegotiationRole role;

  List<NegotiationState> status;

  LocalDate createdAfter;

  LocalDate createdBefore;

  NegotiationSortField sortBy = NegotiationSortField.creationDate;

  Sort.Direction sortOrder = Sort.Direction.DESC;

  @Min(value = 0, message = "Page number must be greater than or equal to 0")
  int page = 0;

  @Min(value = 1, message = "Page size must be greater than or equal to 1")
  int size = 10;
}
