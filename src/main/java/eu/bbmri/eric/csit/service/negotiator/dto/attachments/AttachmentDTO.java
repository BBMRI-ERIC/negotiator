package eu.bbmri.eric.csit.service.negotiator.dto.attachments;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AttachmentDTO {
  @NotNull String id;

  @NotNull String name;
}
