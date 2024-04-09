package eu.bbmri_eric.negotiator.database.model.attachments;

import eu.bbmri_eric.negotiator.database.model.Negotiation;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Person;

public interface BaseAttachmentProjection {
  Person getCreatedBy();

  Negotiation getNegotiation();

  Organization getOrganization();
}
