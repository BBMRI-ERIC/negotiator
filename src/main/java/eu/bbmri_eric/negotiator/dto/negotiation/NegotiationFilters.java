package eu.bbmri_eric.negotiator.dto.negotiation;

import eu.bbmri_eric.negotiator.api.controller.v3.NegotiationRole;
import eu.bbmri_eric.negotiator.configuration.state_machine.negotiation.NegotiationState;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Sort;

/** DTO that defines the possible query filters for Negotiations */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ParameterObject
public class NegotiationRequestParameters {
  @Parameter(
      in = ParameterIn.QUERY,
      name = "role",
      description =
          "Role filter. If specified it returns only the negotiations where the user has the requested role")
  NegotiationRole role;

  @Parameter(
      in = ParameterIn.QUERY,
      name = "state",
      description = "Comma separated values of negotiations states used to filter the negotiations")
  List<NegotiationState> state;

  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdAfter",
      description = "Filter to ask for negotiations created after the specified date (ISO format)")
  LocalDate createdAfter;

  @Parameter(
      in = ParameterIn.QUERY,
      name = "createdBefore",
      description = "Filter to ask for negotiations created before the specified date (ISO format)")
  LocalDate createdBefore;

//  @Parameter(
//      in = ParameterIn.QUERY,
//      name = "sortBy",
//      description =
//          "Parameter to specify the Negotiation attribute to use to sort the list of negotiations returned. By default it uses creationDate")
//  String sortBy = "creationDate";
//
//  @Parameter(
//      in = ParameterIn.QUERY,
//      name = "sortOrder",
//      description = "Parameter to specify the sort order (ASC or DESC). By default it is DESC")
//  Sort.Direction sortOrder = Sort.DEFAULT_DIRECTION;
}
