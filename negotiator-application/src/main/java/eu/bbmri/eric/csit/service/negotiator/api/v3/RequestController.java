package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.response.RequestResponse;
import eu.bbmri.eric.csit.service.negotiator.model.Request;
import eu.bbmri.eric.csit.service.negotiator.service.RequestService;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
public class RequestController {

  @Autowired private RequestService requestService;
  @Autowired private ModelMapper modelMapper;

  /**
   * Create a request and the project it belongs to
   *
   * @param request
   * @return
   */
  @PostMapping(
      value = "/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestResponse add(@Valid @RequestBody RequestRequest request) {
    Request requestEntity = requestService.create(request);
    return modelMapper.map(requestEntity, RequestResponse.class);
  }

  /**
   * Create a request for a specific project
   *
   * @param projectId
   * @param request
   * @return RequestResponse
   */
  @PostMapping(
      value = "/projects/{projectId}/requests",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  RequestResponse add(@PathVariable Long projectId, @Valid @RequestBody RequestRequest request) {
    Request requestEntity = requestService.create(projectId, request);
    return modelMapper.map(requestEntity, RequestResponse.class);
  }

  /**
   * Create a request for a specific project
   *
   * @param request
   * @return RequestResponse
   */
  @PutMapping(
      value = "/requests/{id}",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  RequestResponse update(@Valid @PathVariable Long id, @Valid @RequestBody RequestRequest request) {
    Request requestEntity = requestService.update(id, request);
    return modelMapper.map(requestEntity, RequestResponse.class);
  }

  @GetMapping("/requests")
  List<RequestResponse> list() {
    return requestService.findAll().stream()
        .map(request -> modelMapper.map(request, RequestResponse.class))
        .collect(Collectors.toList());
  }

  @GetMapping("/requests/{id}")
  RequestResponse retrieve(@Valid @PathVariable Long id) {
    Request entity = requestService.findById(id);
    RequestResponse response = modelMapper.map(entity, RequestResponse.class);
    return response;
  }
}
