package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.InformationRequirementCreateDTO;
import eu.bbmri_eric.negotiator.dto.InformationRequirementDTO;
import java.util.List;

/** Service for manipulating information requirements. */
public interface InformationRequirementService {
  /**
   * Create a new information requirement.
   *
   * @param createDTO the DTO containing required information
   * @return the created InformationRequirement
   */
  InformationRequirementDTO createInformationRequirement(InformationRequirementCreateDTO createDTO);

  /**
   * Update an information requirement.
   *
   * @param createDTO the DTO containing required information
   * @return the updated InformationRequirement
   */
  InformationRequirementDTO updateInformationRequirement(InformationRequirementCreateDTO createDTO);

  /**
   * Get a specific information requirement.
   *
   * @param id the id of the information requirement
   * @return the sought information requirement
   */
  InformationRequirementDTO getInformationRequirement(Long id);

  /**
   * Fetch all information requirements.
   *
   * @return all information requirements
   */
  List<InformationRequirementDTO> getAllInformationRequirements();

  /**
   * Delete a specific information requirement.
   *
   * @param id id of the information requirement to delete
   */
  void deleteInformationRequirement(Long id);
}
