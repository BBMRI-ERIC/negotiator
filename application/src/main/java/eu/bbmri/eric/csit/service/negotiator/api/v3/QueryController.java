package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.negotiator.dto.QueryDTO;
import eu.bbmri.eric.csit.service.negotiator.service.DataService;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
public class QueryController {

  private final DataService dataService;

  public QueryController(DataService dataService) {
    this.dataService = dataService;
  }

  @GetMapping("/queries")
  List<Query> list() {
    return dataService.findAllQueries();
  }

  @GetMapping("/queries/{id}")
  Query retrieve(@PathVariable Long id) throws EntityNotFoundException {
    return dataService.getQueryById(id).orElseThrow(EntityNotFoundException::new);
  }

  @PostMapping(
      value = "/queries",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  Query add(@RequestBody QueryDTO query) {
    return dataService.createQuery(query);
  }
}
