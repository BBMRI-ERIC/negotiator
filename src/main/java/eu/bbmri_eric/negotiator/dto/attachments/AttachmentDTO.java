package eu.bbmri_eric.negotiator.dto.attachments;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachmentDTO {
  @NotNull private String id;

  @Nullable private String organizationId;

  @NotNull private String name;

  @NotNull private String contentType;

  @NotNull private Long size;

  private byte[] payload;
}
