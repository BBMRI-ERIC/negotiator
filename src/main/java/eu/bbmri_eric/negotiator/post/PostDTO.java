package eu.bbmri_eric.negotiator.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.user.UserResponseModel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

  @NotNull private String id;

  @Valid @NotEmpty private PostStatus status;

  @Valid @NotEmpty private String text;

  @Valid @NotEmpty private LocalDateTime creationDate;

  @Valid @NotEmpty private UserResponseModel createdBy;

  @Valid private String organizationId;

  @Valid @NotEmpty private PostType type;
}
