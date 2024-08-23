package eu.bbmri_eric.negotiator.form.service;

import eu.bbmri_eric.negotiator.form.dto.SectionCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionMetaDTO;
import java.util.List;

public interface AccessFormsSectionService {
  /**
   * Get all sections.
   *
   * @return a list of all sections
   */
  List<SectionMetaDTO> getAllSections();

  /**
   * Get a specific section.
   *
   * @param id an identifier of the section
   * @return a section
   */
  SectionMetaDTO getSectionById(Long id);

  /**
   * Create a new section.
   *
   * @param sectionCreateDTO containing desired information
   * @return the new section
   */
  SectionMetaDTO createSection(SectionCreateDTO sectionCreateDTO);

  /**
   * Update an existing section or create a new one.
   *
   * @param sectionCreateDTO containing desired information
   * @param id the identifier of the updated section
   * @return updated section or a new one if an existing one with the provided id wasn't found
   */
  SectionMetaDTO updateSection(SectionCreateDTO sectionCreateDTO, Long id);
}
