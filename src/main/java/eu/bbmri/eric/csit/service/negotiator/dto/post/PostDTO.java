package eu.bbmri.eric.csit.service.negotiator.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import eu.bbmri.eric.csit.service.negotiator.dto.person.PersonDTO;
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
public class PostDTO {

  Set<Attachment> attachments;
  @NotNull private String id;
  @Valid @NotEmpty private PostStatus status;
  @Valid @NotEmpty private String text;

  @Valid @NotEmpty private LocalDateTime creationDate;

  @Valid @NotEmpty private LocalDateTime modifiedDate;

  @Valid @NotEmpty private PersonDTO poster;

  @Valid private String resourceId;

  @Valid @NotEmpty private PostType type;
}
