package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_form.SectionCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.SectionMetaDTO;
import java.util.List;

public interface AccessFormsSectionService {
  List<SectionMetaDTO> getAllSections();

  SectionMetaDTO getSectionById(Long id);

  SectionMetaDTO createSection(SectionCreateDTO section);

  SectionMetaDTO updateSection(SectionCreateDTO section, Long id);
}
