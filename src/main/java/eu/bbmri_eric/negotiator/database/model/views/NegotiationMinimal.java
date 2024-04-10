package eu.bbmri_eric.negotiator.database.model.views;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.IdMapping;
import eu.bbmri_eric.negotiator.database.model.Negotiation;

@EntityView(Negotiation.class)
public interface NegotiationMinimal {
  @IdMapping
  String getId();
}
