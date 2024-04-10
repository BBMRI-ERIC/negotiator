package eu.bbmri_eric.negotiator.database.model.attachment;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import eu.bbmri_eric.negotiator.database.model.Organization;

@EntityView(Attachment.class)
public interface MetadataAttachmentProjection {
  @IdMapping
  String getId();

  String getName();

  String getSize();

  String getContentType();

  OrganizationMinimal getOrganization();

  @EntityView(Organization.class)
  interface OrganizationMinimal {
    @IdMapping
    String getId();

    String getExternalId();
  }
}
