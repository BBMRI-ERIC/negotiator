package eu.bbmri_eric.negotiator.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri_eric.negotiator.attachment.Attachment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
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
