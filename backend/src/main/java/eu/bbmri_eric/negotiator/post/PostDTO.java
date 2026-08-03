package eu.bbmri_eric.negotiator.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Relation(itemRelation = "post", collectionRelation = "posts")
public class PostDTO {
  @NonNull private String id;
  @NonNull private String text;
  @NonNull private LocalDateTime creationDate;
  @NonNull private UserResponseModel createdBy;

  private String organizationId;

  private PostType type = PostType.PUBLIC;

  @Schema(
      description =
          "Display name of the help desk actor who created this post (e.g. the representative's name or email). "
              + "Only accepted from callers holding ROLE_HELPDESK_INTEGRATION. Stored for display purposes only.",
      example = "john.smith@mhh.de")
  private String helpdeskActor;
}
