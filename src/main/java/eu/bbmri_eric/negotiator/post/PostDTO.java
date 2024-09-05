package eu.bbmri_eric.negotiator.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;
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
public class PostDTO {

  @NotEmpty private String id;

  @NotEmpty private PostStatus status;

  @NotEmpty private String text;

  @NotEmpty private LocalDateTime creationDate;

  @NotEmpty private UserResponseModel createdBy;

  private String organizationId;

  @NotEmpty private PostType type;
}
