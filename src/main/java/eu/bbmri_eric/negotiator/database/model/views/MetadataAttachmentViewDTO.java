package eu.bbmri_eric.negotiator.database.model.views;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MetadataAttachmentViewDTO {
  private String id;
  private String name;
  private Long size;
  private String contentType;
  @Nullable private Long organizationId;
  @Nullable private String organizationExternalId;
  private String negotiationId;
  private Long createdById;
}
