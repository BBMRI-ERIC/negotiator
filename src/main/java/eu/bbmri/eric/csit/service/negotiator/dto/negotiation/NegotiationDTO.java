package eu.bbmri.eric.csit.service.negotiator.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri.eric.csit.service.negotiator.dto.attachments.AttachmentMetadataDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.resource.ResourceWithStatusDTO;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

  @NotNull private Boolean postsEnabled;

  @NotNull private LocalDateTime creationDate;

  @NotNull private LocalDateTime modifiedDate;

  private Set<AttachmentMetadataDTO> attachments;

  public Set<ResourceWithStatusDTO> resources;

  @JsonIgnore
  public String getStatusForResource(String resourceId) {
    Optional<ResourceWithStatusDTO> resource =
        this.resources.stream().filter(r -> Objects.equals(r.getId(), resourceId)).findFirst();
      return resource.map(ResourceWithStatusDTO::getStatus).orElse(null);
  }
}
