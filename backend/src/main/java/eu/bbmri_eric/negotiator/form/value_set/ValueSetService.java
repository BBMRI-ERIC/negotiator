package eu.bbmri_eric.negotiator.form.value_set;

import java.util.List;

public interface ValueSetService {
  /**
   * Fetch all value sets.
   *
   * @return a list of all value sets.
   */
  List<ValueSetDTO> getAllValueSets();

  /**
   * Fetch a specific value set by its id.
   *
   * @param id the identifier of the value set
   * @return the value set
   */
  ValueSetDTO getValueSetById(long id);

  /**
   * Create a new value set.
   *
   * @param createDTO creation object containing all necessary information
   * @return the created value set
   */
  ValueSetDTO createValueSet(ValueSetCreateDTO createDTO);

  /**
   * Update a value set, or create a new one if there is none associated with the provided id.
   *
   * @param createDTO creation object containing all necessary information
   * @param id the identifier of the value set
   * @return the updated/created value set
   */
  ValueSetDTO updateValueSet(ValueSetCreateDTO createDTO, Long id);
}
