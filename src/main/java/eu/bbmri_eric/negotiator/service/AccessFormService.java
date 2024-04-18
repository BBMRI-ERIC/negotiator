package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementLinkDTO;
import eu.bbmri_eric.negotiator.dto.access_form.FormCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.SectionLinkDTO;
import org.springframework.data.domain.Pageable;

public interface AccessFormService {
  /**
   * Get an access form combining the access criteria of the resources in the request.
   *
   * @param requestId The request id.
   * @return The access form.
   */
  AccessFormDTO getAccessFormForRequest(String requestId);

  /**
   * Get an access form by id.
   *
   * @param id The access form id.
   * @return The access form.
   */
  AccessFormDTO getAccessForm(Long id);

  /**
   * Get all access forms.
   *
   * @param pageable object specifying retrieval parameters.
   * @return a group of Access-forms
   */
  Iterable<AccessFormDTO> getAllAccessForms(Pageable pageable);

  /**
   * Create a new Access-form.
   *
   * @param createDTO creation request object
   * @return the newly created Access-form
   */
  AccessFormDTO createAccessForm(FormCreateDTO createDTO);

  /**
   * Add a section to a specific Access-form.
   *
   * @param linkDTO object specifying additional linking parameters
   * @param formId the identifier of the Access-form to be modified
   * @return the modified Access-form
   */
  AccessFormDTO addSection(SectionLinkDTO linkDTO, Long formId);

  /**
   * Add an element to a specific section of a given Access-form.
   *
   * @param linkDTO object specifying additional linking parameters
   * @param formId the identifier of the Access-form to be modified
   * @param sectionId the identifier of the Section to be modified
   * @return the modified Access-form
   */
  AccessFormDTO addElement(ElementLinkDTO linkDTO, Long formId, Long sectionId);
}
