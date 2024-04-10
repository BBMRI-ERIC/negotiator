package eu.bbmri_eric.negotiator.database.model.views;

import com.blazebit.persistence.view.EntityView;
import eu.bbmri_eric.negotiator.database.model.Attachment;

@EntityView(Attachment.class)
public interface AttachmentView extends MetadataAttachmentView {
  byte[] getPayload();
}
