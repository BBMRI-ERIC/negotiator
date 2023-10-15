package eu.bbmri_eric.negotiator.dto.attachments;

import jakarta.validation.constraints.NotNull;
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
