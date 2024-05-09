package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.MolgenisBiobank;
import eu.bbmri_eric.negotiator.dto.MolgenisCollection;
import java.util.List;
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

  /**
   * Fetch all BBMRI-ERIC, MIABIS defined Biobanks.
   *
   * @return Optional List<MolgenisBiobank>
   */
  List<MolgenisBiobank> findAllBiobanks();

  /**
   * Fetch all BBMRI-ERIC, MIABIS defined Collections by their Biobank Id.
   *
   * @param id Identifier of the biobank
   * @return Optional String<MolgenisBiobank>
   */
  List<MolgenisCollection> findAllCollectionsByBiobankId(String biobankId);
}
