package eu.bbmri_eric.negotiator.database.model.attachment;

public interface AttachmentProjection extends MetadataAttachmentProjection {
  byte[] getPayload();
}
