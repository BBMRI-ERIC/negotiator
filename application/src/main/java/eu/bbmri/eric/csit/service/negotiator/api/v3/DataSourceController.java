package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.dto.ValidationGroups.Create;
import eu.bbmri.eric.csit.service.negotiator.dto.ValidationGroups.Update;
import eu.bbmri.eric.csit.service.negotiator.dto.request.DataSourceRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.response.DataSourceResponse;
import eu.bbmri.eric.csit.service.negotiator.service.DataSourceService;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
@Validated
public class DataSourceController {
  @Autowired private DataSourceService dataSourceService;
  @Autowired private ModelMapper modelMapper;

  @GetMapping("/data-sources")
  List<DataSourceResponse> list() {
    return dataSourceService.findAll().stream()
        .map(dataSource -> modelMapper.map(dataSource, DataSourceResponse.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/data-sources/{id}")
  DataSourceResponse retrieve(@PathVariable Long id) {
    return modelMapper.map(dataSourceService.getById(id), DataSourceResponse.class);
  }

  @PostMapping(
      value = "/data-sources",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DataSourceResponse add(@Validated(Create.class) @RequestBody DataSourceRequest request) {
    DataSource dataSourceEntity = dataSourceService.create(request);
    return modelMapper.map(dataSourceEntity, DataSourceResponse.class);
  }

  @PutMapping(
      value = "/data-sources/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  DataSourceResponse update(
      @PathVariable Long id, @Validated(Update.class) @RequestBody DataSourceRequest request) {
    DataSource dataSourceEntity = dataSourceService.update(id, request);
    return modelMapper.map(dataSourceEntity, DataSourceResponse.class);
  }
}
