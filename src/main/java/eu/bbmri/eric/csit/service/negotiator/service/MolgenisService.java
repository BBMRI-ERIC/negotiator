package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisCollection;

public interface MolgenisService {
  /**
   * Verify if Molgenis is reachable.
   *
   * @return true | false
   */
  public boolean isReachable();

  public MolgenisCollection findCollectionById(String id);
}
