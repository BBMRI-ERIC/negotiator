package eu.bbmri.eric.csit.service.negotiator.api.common;

import eu.bbmri.eric.csit.service.negotiator.dto.request.PerunUserRequest;
import eu.bbmri.eric.csit.service.negotiator.service.PersonService;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/perun")
public class PerunController {

  @Autowired private PersonService personService;
  @Autowired private ModelMapper modelMapper;

  @PostMapping(
      value = "/users",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  List<PerunUserRequest> createOrUpdate(
      @RequestBody @NotEmpty(message = "Perun Users Request list cannot be empty.")
          List<@Valid PerunUserRequest> perunUsersRequest) {

    for (PerunUserRequest request : perunUsersRequest) {
      personService.createOrUpdate(request);
    }
    return perunUsersRequest;
  }
}
