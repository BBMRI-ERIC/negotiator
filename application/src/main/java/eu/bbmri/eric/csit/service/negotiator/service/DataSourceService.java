package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DataSourceService {

  @Autowired private DataSourceRepository dataSourceRepository;

  @Autowired private ModelMapper modelMapper;

  public DataSource getById(Long id) {
    return dataSourceRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, String.format("Data source with id %d not found", id)));
  }

  public List<DataSource> findAll() {
    return dataSourceRepository.findAll();
  }

  public DataSource create(DataSourceRequest dataSourceRequest) {
    DataSource dataSourceEntity = modelMapper.map(dataSourceRequest, DataSource.class);
    try {
      return dataSourceRepository.save(dataSourceEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be saved");
    }
  }

  public DataSource update(Long id, DataSourceRequest dataSourceRequest) {
    DataSource dataSourceEntity = getById(id);
    modelMapper.map(dataSourceRequest, dataSourceEntity);
    try {
      return dataSourceRepository.save(dataSourceEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be saved");
    }
  }
}
