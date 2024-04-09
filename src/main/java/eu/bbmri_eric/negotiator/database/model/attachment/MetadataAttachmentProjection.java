package eu.bbmri_eric.negotiator.database.model.attachment;

public interface MetadataAttachmentProjection extends BaseAttachmentProjection {
  String getId();

  String getName();

  String getSize();

  String getContentType();
}
