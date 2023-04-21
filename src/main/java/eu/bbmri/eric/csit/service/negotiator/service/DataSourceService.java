package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.DataSource;
import java.util.List;

public interface DataSourceService {

  DataSourceDTO create(DataSourceCreateDTO dataSourceRequest);

  DataSourceDTO update(Long id, DataSourceCreateDTO dataSourceRequest);

  List<DataSourceDTO> findAll();

  DataSourceDTO findById(Long id);
}
