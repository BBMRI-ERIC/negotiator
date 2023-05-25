package eu.bbmri.eric.csit.service.negotiator.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NegotiationDTO {

  @NotNull
  private String id;

  private Set<RequestDTO> requests;

  private Set<PersonRoleDTO> persons;

  @NotNull
  private JsonNode payload;

  @NotNull
  private String status;

  @NotNull
  private JsonNode resourceStatus;
}
