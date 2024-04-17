package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_form.ElementCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import java.util.List;

public interface AccessFormElementService {
  /**
   * Get all elements.
   *
   * @return a list of all elements
   */
  List<ElementMetaDTO> getAllElements();

  /**
   * Get a specific element by its identifier.
   *
   * @param id the identifier of the element
   * @return an element
   */
  ElementMetaDTO getElementById(Long id);

  /**
   * Create a new element.
   *
   * @param elementCreateDTO containing desired information
   * @return the created element
   */
  ElementMetaDTO createElement(ElementCreateDTO elementCreateDTO);

  /**
   * Update an existing element or create a new one.
   *
   * @param elementCreateDTO containing desired information
   * @param id the identifier of the updated element
   * @return updated element or a new one if an existing one with the provided id wasn't found
   */
  ElementMetaDTO updateElement(ElementCreateDTO elementCreateDTO, Long id);
}
