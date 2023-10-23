package eu.bbmri.eric.csit.service.negotiator.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostStatus;
import eu.bbmri.eric.csit.service.negotiator.database.model.PostType;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
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
public class PostCreateDTO {

  Set<Attachment> attachments;
  
  @Valid @NotEmpty private String text;

  @Valid private String organizationId;

  @Valid private PostStatus status;

  @Valid private PostType type;
}
