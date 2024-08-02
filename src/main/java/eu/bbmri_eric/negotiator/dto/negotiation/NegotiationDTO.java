package eu.bbmri_eric.negotiator.dto.negotiation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.negotiator.dto.person.UserResponseModel;
import eu.bbmri_eric.negotiator.dto.request.RequestMinimalDTO;
import eu.bbmri_eric.negotiator.dto.resource.ResourceWithStatusDTO;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
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
  @JsonIgnore public Set<ResourceWithStatusDTO> resources;
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
        this.resources.stream()
            .filter(r -> Objects.equals(r.getExternalId(), resourceId))
            .findFirst();
    if (resource.isPresent()) {
      try {
        return resource.get().getStatus().toString();
      } catch (NullPointerException e) {
        return "";
      }
    }
    return "";
  }
}
