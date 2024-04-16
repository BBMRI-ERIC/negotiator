package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_form.ElementCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import java.util.List;

public interface AccessFormElementService {
  List<ElementMetaDTO> getAll();

  ElementMetaDTO getById(Long id);

  ElementMetaDTO create(ElementCreateDTO dto);

  ElementMetaDTO update(ElementCreateDTO dto, Long id);
}
