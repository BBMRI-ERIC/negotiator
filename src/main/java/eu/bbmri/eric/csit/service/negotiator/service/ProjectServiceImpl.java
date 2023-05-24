package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.service.dto.project.ProjectCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.service.dto.project.ProjectDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Project;
import eu.bbmri.eric.csit.service.negotiator.database.repository.ProjectRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotStorableException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service(value = "DefaultProjectService")
public class ProjectServiceImpl implements ProjectService {

  @Autowired
  private ProjectRepository projectRepository;
  @Autowired
  private ModelMapper modelMapper;

  public ProjectDTO create(ProjectCreateDTO projectBody) throws EntityNotStorableException {
    Project projectEntity = modelMapper.map(projectBody, Project.class);
    try {
      Project project = projectRepository.save(projectEntity);
      return modelMapper.map(project, ProjectDTO.class);
    } catch (DataIntegrityViolationException ex) {
      throw new EntityNotStorableException();
    }
  }

  @Transactional
  public ProjectDTO findById(String id) throws EntityNotFoundException {
    Project project = projectRepository
        .findDetailedById(id)
        .orElseThrow(() -> new EntityNotFoundException(id));
    return modelMapper.map(project, ProjectDTO.class);
  }

  @Transactional
  public List<ProjectDTO> findAll() {
    return projectRepository.findAll().stream()
        .map(project -> modelMapper.map(project, ProjectDTO.class))
        .collect(Collectors.toList());
  }
}
