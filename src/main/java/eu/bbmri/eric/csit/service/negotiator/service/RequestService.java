package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import java.util.List;
import java.util.Set;

public interface RequestService {

  RequestDTO create(RequestCreateDTO requestBody);

  List<RequestDTO> findAll();

  RequestDTO findById(String id);

  Set<RequestDTO> findAllById(Set<String> ids);

  RequestDTO update(String id, RequestCreateDTO requestBody);
}
