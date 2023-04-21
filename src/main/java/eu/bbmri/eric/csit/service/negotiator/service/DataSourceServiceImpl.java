package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.database.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DataSourceServiceImpl implements DataSourceService {

  private final DataSourceRepository dataSourceRepository;

  private final ModelMapper modelMapper;

  @Autowired
  public DataSourceServiceImpl(DataSourceRepository dataSourceRepository, ModelMapper modelMapper) {
    this.dataSourceRepository = dataSourceRepository;
    this.modelMapper = modelMapper;
  }

  private DataSource findEntityById(Long id) {
    return dataSourceRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  @Transactional
  public DataSourceDTO create(DataSourceCreateDTO dataSourceRequest) {
    DataSource dataSourceEntity = modelMapper.map(dataSourceRequest, DataSource.class);
    try {
      return modelMapper.map(dataSourceRepository.save(dataSourceEntity), DataSourceDTO.class);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public DataSourceDTO update(Long id, DataSourceCreateDTO dataSourceRequest) {
    DataSource dataSourceEntity = findEntityById(id);
    modelMapper.map(dataSourceRequest, dataSourceEntity);
    try {
      return modelMapper.map(dataSourceRepository.save(dataSourceEntity), DataSourceDTO.class);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  public DataSourceDTO findById(Long id) {
    DataSource dataSource = findEntityById(id);
    return modelMapper.map(dataSource, DataSourceDTO.class);
  }

  public List<DataSourceDTO> findAll() {
    return dataSourceRepository.findAll().stream()
        .map(dataSource -> modelMapper.map(dataSource, DataSourceDTO.class))
        .collect(Collectors.toList());
  }

}
