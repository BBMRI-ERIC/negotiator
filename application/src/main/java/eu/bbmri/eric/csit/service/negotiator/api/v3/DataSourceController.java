package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
import java.util.List;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
public class DataSourceController {

  private final DataSourceRepository dataSourceRepository;

  DataSourceController(DataSourceRepository dataSourceRepository) {
    this.dataSourceRepository = dataSourceRepository;
  }

  @GetMapping("/data-sources")
  List<DataSource> list() {
    return dataSourceRepository.findAll();
  }

  @GetMapping("/data-sources/{id}")
  DataSource retrieve(@PathVariable Long id) throws NotFoundException {
    return dataSourceRepository.findById(id).orElseThrow(NotFoundException::new);
  }
}
