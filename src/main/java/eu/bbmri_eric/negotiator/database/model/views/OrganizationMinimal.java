package eu.bbmri_eric.negotiator.database.model.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import eu.bbmri_eric.negotiator.database.model.Organization;

@EntityView(Organization.class)
public interface OrganizationMinimal {
  @IdMapping
  Long getId();

  String getExternalId();
}
