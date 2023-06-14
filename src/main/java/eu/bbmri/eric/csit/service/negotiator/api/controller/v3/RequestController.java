package eu.bbmri.eric.csit.service.negotiator.api.controller.v3;

import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
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
@CrossOrigin
public class RequestController {

  @Autowired
  private RequestService requestService;

  @GetMapping("/requests")
  List<RequestDTO> list() {
    return requestService.findAll();
  }

  @GetMapping("/requests/{id}")
  RequestDTO retrieve(@PathVariable String id) {
    return requestService.findById(id);
  }

  @PostMapping(
      value = "/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestDTO add(@Valid @RequestBody RequestCreateDTO queryRequest) {
    return requestService.create(queryRequest);
  }

  @PutMapping(
      value = "/requests/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  RequestDTO update(
      @Valid @PathVariable String id, @Valid @RequestBody RequestCreateDTO queryRequest) {
    return requestService.update(id, queryRequest);
  }
}
