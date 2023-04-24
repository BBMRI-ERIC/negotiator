package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.api.dto.ValidationGroups.Create;
import eu.bbmri.eric.csit.service.negotiator.api.dto.ValidationGroups.Update;
import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.datasource.DataSourceDTO;
import eu.bbmri.eric.csit.service.negotiator.service.DataSourceService;
import java.util.List;
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

  @Autowired
  private DataSourceService dataSourceService;
  @Autowired
  private ModelMapper modelMapper;

  @GetMapping("/data-sources")
  List<DataSourceDTO> list() {
    return dataSourceService.findAll();
  }

  @GetMapping("/data-sources/{id}")
  DataSourceDTO retrieve(@PathVariable Long id) {
    return dataSourceService.findById(id);
  }

  @PostMapping(
      value = "/data-sources",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  DataSourceDTO add(@Validated(Create.class) @RequestBody DataSourceCreateDTO request) {
    return dataSourceService.create(request);
  }

  @PutMapping(
      value = "/data-sources/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  DataSourceDTO update(
      @PathVariable Long id, @Validated(Update.class) @RequestBody DataSourceCreateDTO request) {
    return dataSourceService.update(id, request);
  }
}
