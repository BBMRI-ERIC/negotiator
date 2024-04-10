package eu.bbmri_eric.negotiator.database.model.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import eu.bbmri_eric.negotiator.database.model.Resource;

@EntityView(Resource.class)
public interface ResourceMinimal {
  @IdMapping
  Long getId();

  String getSourceId();
}
