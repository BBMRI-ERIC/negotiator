package eu.bbmri_eric.negotiator.negotiation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
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
    return "";
  }
}
