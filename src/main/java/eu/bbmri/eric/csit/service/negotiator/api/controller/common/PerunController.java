package eu.bbmri.eric.csit.service.negotiator.api.controller.common;

import eu.bbmri.eric.csit.service.negotiator.api.dto.perun.PerunUserDTO;
import eu.bbmri.eric.csit.service.negotiator.service.PersonService;
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

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Validated
@RestController
@RequestMapping("/perun")
public class PerunController {

  @Autowired
  private PersonService personService;
  @Autowired
  private ModelMapper modelMapper;

  @PostMapping(
      value = "/users",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  List<PerunUserDTO> createOrUpdate(
      @RequestBody @NotEmpty(message = "Perun Users Negotiation list cannot be empty.")
      List<@Valid PerunUserDTO> request) {
    return personService.createOrUpdate(request);
  }
}
