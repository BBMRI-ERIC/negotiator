package eu.bbmri.eric.csit.service.negotiator.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import eu.bbmri.eric.csit.service.negotiator.dto.person.UserResponseModel;
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
