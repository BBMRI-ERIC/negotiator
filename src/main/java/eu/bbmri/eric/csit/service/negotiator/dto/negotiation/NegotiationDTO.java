package eu.bbmri.eric.csit.service.negotiator.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import java.time.LocalDateTime;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class NegotiationDTO {

  @NotNull private String id;

  private Set<RequestDTO> requests;

  private Set<PersonRoleDTO> persons;

  @NotNull private JsonNode payload;

  @NotNull private String status;

  @NotNull private JsonNode resourceStatus;

  @NotNull private Boolean postsEnabled;

  @NotNull private LocalDateTime creationDate;

  @NotNull private LocalDateTime modifiedDate;
}
