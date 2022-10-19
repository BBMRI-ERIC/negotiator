package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.negotiator.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.repository.DataSourceRepository;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class DataSourceService {

  @Autowired private DataSourceRepository dataSourceRepository;

  @Autowired private ModelMapper modelMapper;

  public DataSource getById(Long id) {
    return dataSourceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  public List<DataSource> findAll() {
    return dataSourceRepository.findAll();
  }

  public DataSource create(DataSourceRequest dataSourceRequest) {
    DataSource dataSourceEntity = modelMapper.map(dataSourceRequest, DataSource.class);
    try {
      return dataSourceRepository.save(dataSourceEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public DataSource update(Long id, DataSourceRequest dataSourceRequest) {
    DataSource dataSourceEntity = getById(id);
    modelMapper.map(dataSourceRequest, dataSourceEntity);
    try {
      return dataSourceRepository.save(dataSourceEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }
}
