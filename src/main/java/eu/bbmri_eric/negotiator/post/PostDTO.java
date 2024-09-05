package eu.bbmri_eric.negotiator.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
public class PostDTO {
  @NonNull private String id;
  @NonNull private PostStatus status;
  @NonNull private String text;
  @NonNull private LocalDateTime creationDate;
  @NonNull private UserResponseModel createdBy;

  private String organizationId;

  private PostType type = PostType.PUBLIC;
}
