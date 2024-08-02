package eu.bbmri_eric.negotiator.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.negotiator.dto.person.UserResponseModel;
import eu.bbmri_eric.negotiator.dto.request.RequestMinimalDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestMinimalDTO;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;
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

  @NotNull private String id;
  private UserResponseModel author;
  private Set<RequestMinimalDTO> requests;
  @NotNull private JsonNode payload;
  @NotNull private String status;
  @NotNull private boolean publicPostsEnabled;
  @NotNull private boolean privatePostsEnabled;
  @NotNull private LocalDateTime creationDate;
  @NotNull private LocalDateTime modifiedDate;

  @JsonIgnore
  public String getStatusForResource(String resourceId) {
    Optional<ResourceWithStatusDTO> resource =
        this.resources.stream().filter(r -> Objects.equals(r.getId(), resourceId)).findFirst();
    return resource.map(ResourceWithStatusDTO::getStatus).orElse(null);
  }
}
