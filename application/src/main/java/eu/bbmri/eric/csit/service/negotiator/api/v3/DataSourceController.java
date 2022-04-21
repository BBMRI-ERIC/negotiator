package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.response.DataSourceResponse;
import eu.bbmri.eric.csit.service.negotiator.service.DataSourceService;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
public class DataSourceController {
  @Autowired private DataSourceRepository dataSourceRepository;
  @Autowired private DataSourceService dataSourceService;
  @Autowired private ModelMapper modelMapper;

  @GetMapping("/data-sources")
  List<DataSourceResponse> list() {
    return dataSourceRepository.findAll().stream()
        .map(dataSource -> modelMapper.map(dataSource, DataSourceResponse.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/data-sources/{id}")
  DataSourceResponse retrieve(@PathVariable Long id) throws NotFoundException {
    return modelMapper.map(
        dataSourceService.getById(id),
        DataSourceResponse.class);
  }

  @PostMapping("/data-sources")
  DataSourceResponse add(@Valid @RequestBody DataSourceRequest request) throws NotFoundException {
    DataSource dataSourceEntity = dataSourceService.create(request);
    return modelMapper.map(dataSourceEntity, DataSourceResponse.class);
  }
}
