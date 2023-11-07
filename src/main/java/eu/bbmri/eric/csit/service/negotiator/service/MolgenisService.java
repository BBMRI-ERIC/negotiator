package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisBiobank;
import eu.bbmri.eric.csit.service.negotiator.dto.MolgenisCollection;
import java.util.Optional;

public interface MolgenisService {
  /**
   * Verify if Molgenis is reachable.
   *
   * @return true | false
   */
  boolean isReachable();

  /**
   * Fetch a BBMRI-ERIC, MIABIS defined collection by its ID.
   *
   * @param id Identifier of the collection
   * @return Optional<MolgenisCollection>
   */
  Optional<MolgenisCollection> findCollectionById(String id);

  /**
   * Fetch a BBMRI-ERIC, MIABIS defined biobank by its ID.
   *
   * @param id Identifier of the biobank
   * @return Optional<MolgenisBiobank>
   */
  Optional<MolgenisBiobank> findBiobankById(String id);
}
