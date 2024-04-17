package eu.bbmri_eric.negotiator.database.model.views;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttachmentViewDTO extends MetadataAttachmentViewDTO {
  private byte[] payload;

  public AttachmentViewDTO(
      String id,
      String name,
      Long size,
      String contentType,
      byte[] payload,
      Long organizationId,
      String organizationExternalId,
      String negotiationId,
      Long personId) {
    super(
        id,
        name,
        size,
        contentType,
        organizationId,
        organizationExternalId,
        negotiationId,
        personId);
    this.payload = payload;
  }
}
