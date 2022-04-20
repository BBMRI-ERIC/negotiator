package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.model.Project;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.repository.PersonRepository;
import eu.bbmri.eric.csit.service.repository.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {

  @Autowired private ProjectRepository projectRepository;

  public Project create(ProjectRequest projectRequest) {
    Project projectEntity = new Project();
    projectEntity.setTitle(projectRequest.getTitle());
    projectEntity.setDescription(projectRequest.getDescription());
    projectEntity.setEthicsVote(projectRequest.getEthicsVote());
    projectEntity.setIsTestProject(projectRequest.getIsTestProject());
    projectEntity.setExpectedDataGeneration(projectRequest.getExpectedDataGeneration());
    projectEntity.setExpectedEndDate(projectRequest.getExpectedEndDate());
    try {
      return projectRepository.save(projectEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Some data sent cannot be saved");
    }
  }
}
