package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.Project;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.response.ProjectResponse;
import eu.bbmri.eric.csit.service.negotiator.service.ProjectService;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v3")
public class ProjectController {

  @Autowired private ProjectService projectService;

  @Autowired private ModelMapper modelMapper;

  @PostMapping(
      value = "/projects",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  ProjectResponse add(@Valid @RequestBody ProjectRequest request) {
    Project projectEntity = projectService.createQuery(request);
    return modelMapper.map(projectEntity, ProjectResponse.class);
  }
}
