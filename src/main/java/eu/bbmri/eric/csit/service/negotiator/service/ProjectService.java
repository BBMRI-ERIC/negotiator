package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.project.ProjectDTO;
import java.util.List;

public interface ProjectService {

  ProjectDTO create(ProjectCreateDTO projectRequest);

  ProjectDTO findById(String id);

  List<ProjectDTO> findAll();

}
