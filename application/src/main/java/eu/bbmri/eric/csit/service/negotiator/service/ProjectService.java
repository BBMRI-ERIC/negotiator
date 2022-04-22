package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.model.Project;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ProjectRequest;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import eu.bbmri.eric.csit.service.repository.ProjectRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ProjectService {

  @Autowired private ProjectRepository projectRepository;
  @Autowired private ModelMapper modelMapper;

  public Project create(ProjectRequest projectRequest) {
    Project projectEntity = modelMapper.map(projectRequest, Project.class);
    try {
      return projectRepository.save(projectEntity);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }
}
