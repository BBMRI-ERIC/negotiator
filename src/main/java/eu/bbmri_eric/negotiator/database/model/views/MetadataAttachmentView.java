package eu.bbmri_eric.negotiator.database.model.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import com.blazebit.persistence.view.Mapping;
import eu.bbmri_eric.negotiator.database.model.Attachment;
import java.io.Serializable;

@EntityView(Attachment.class)
public interface MetadataAttachmentView extends Serializable {
  @IdMapping
  String getId();

  @Mapping("name")
  String getName();

  Long getSize();

  String getContentType();

  OrganizationMinimal getOrganization();

  NegotiationMinimal getNegotiation();

  PersonMinimal getCreatedBy();
}
