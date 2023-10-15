package eu.bbmri_eric.negotiator.service;

import eu.bbmri_eric.negotiator.dto.datasource.DataSourceCreateDTO;
import eu.bbmri_eric.negotiator.dto.datasource.DataSourceDTO;
import eu.bbmri_eric.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.exceptions.EntityNotStorableException;
import java.util.List;

public interface DataSourceService {

  /**
   * Creates a new DataSource in the Negotiator and returns the newly created record
   *
   * @param dataSourceCreateDTO a DataSourceCreateDTO with the data of the DataSource to create
   * @return a DataSourceDTO with the data of the newly created DataSource
   * @throws EntityNotStorableException if some error occurs when creating the DataSource
   */
  DataSourceDTO create(DataSourceCreateDTO dataSourceCreateDTO) throws EntityNotStorableException;

  /**
   * Update the DataSource with id
   *
   * @param id the id of the DataSource to update
   * @param dataSourceCreateDTO a DataSourceCreateDTO with the new data of the DataSource to updated
   * @return the DataSourceDTO with the updated data of the DataSource
   */
  DataSourceDTO update(Long id, DataSourceCreateDTO dataSourceCreateDTO)
      throws EntityNotStorableException;

  /**
   * Returns the list of all DataSourceDTOs in the Negotiator
   *
   * @return List of all DataSourceDTO in the negotiator
   */
  List<DataSourceDTO> findAll();

  /**
   * Retrieve the DataSource with the :id requested
   *
   * @param id the id of the DataSource
   * @return a DataSourceDTOs with the data of the DataSource with the id requested
   * @throws EntityNotFoundException if the DataSource identified by :id is not found
   */
  DataSourceDTO findById(Long id) throws EntityNotFoundException;
}
