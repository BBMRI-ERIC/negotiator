package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

@org.springframework.stereotype.Service
public class DataSourceService {

  @Autowired
  private DataSourceRepository dataSourceRepository;

  @Autowired
  private ModelMapper modelMapper;

  public DataSource getById(Long id) {
    return dataSourceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  public List<DataSource> findAll() {
    return dataSourceRepository.findAll();
  }

  public DataSource create(DataSourceCreateDTO dataSourceRequest) {
    DataSource dataSourceEntity = modelMapper.map(dataSourceRequest, DataSource.class);
    try {
      return dataSourceRepository.save(dataSourceEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public DataSource update(Long id, DataSourceCreateDTO dataSourceRequest) {
    DataSource dataSourceEntity = getById(id);
    modelMapper.map(dataSourceRequest, dataSourceEntity);
    try {
      return dataSourceRepository.save(dataSourceEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }
}
