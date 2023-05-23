package eu.bbmri.eric.csit.service.negotiator.api.dto.post;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.person.PersonRoleDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Attachment;
import eu.bbmri.eric.csit.service.negotiator.database.model.Person;
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
  @NotNull
  private String id;
  Set<Attachment> attachments;
  @Valid
  @NotEmpty
  private String status;
  @Valid
  @NotEmpty
  private String text;

  @Valid
  @NotEmpty
  private LocalDateTime creationDate;

  @Valid
  @NotEmpty
  private LocalDateTime modifiedDate;

  @Valid
  @NotEmpty
  private PersonDTO poster;


}
