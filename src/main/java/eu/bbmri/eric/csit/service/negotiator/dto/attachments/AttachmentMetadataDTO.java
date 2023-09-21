package eu.bbmri.eric.csit.service.negotiator.dto.attachments;

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
public class AttachmentMetadataDTO {
  @NotNull String id;

  String name;

  String contentType;

  Long size;
}
