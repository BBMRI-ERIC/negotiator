package eu.bbmri_eric.negotiator.database.model.attachments;

public interface AttachmentProjection extends MetadataAttachmentProjection {
    byte[] getPayload();
}
