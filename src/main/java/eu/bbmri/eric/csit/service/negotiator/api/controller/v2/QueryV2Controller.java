package eu.bbmri.eric.csit.service.negotiator.api.controller.v2;

import eu.bbmri.eric.csit.service.negotiator.api.dto.negotiation.NegotiationDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryCreateV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.QueryV2DTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.service.NegotiationServiceImpl;
import eu.bbmri.eric.csit.service.negotiator.service.RequestServiceImpl;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueryV2Controller {

  @Autowired
  private RequestServiceImpl requestService;
  @Autowired
  private NegotiationServiceImpl negotiationService;
  @Autowired
  private ModelMapper modelMapper;

  @PostMapping(
      value = "/directory/create_query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<QueryV2DTO> add(@Valid @RequestBody QueryCreateV2DTO queryRequest) {
    RequestCreateDTO v3Request = modelMapper.map(queryRequest, RequestCreateDTO.class);
    RequestDTO requestResponse;
    boolean created;
    if (queryRequest.getToken() != null && !queryRequest.getToken().isEmpty()) {
      // Update an old request or add a new one to a negotiation
      String[] tokens = queryRequest.getToken().split("__search__");
      // If the negotiation was not found in V2, a new request was created
      if (negotiationService.exists(tokens[0])) {
        created = false;
        if (tokens.length == 1) {
          requestResponse = requestService.create(v3Request);
          NegotiationDTO negotiationDTO = negotiationService.addRequestToNegotiation(tokens[0], requestResponse.getId());
          requestResponse.setNegotiationId(negotiationDTO.getId());
        } else { // Updating an old request: the requestToken can be ignored
          requestResponse = requestService.update(tokens[1], v3Request);
        }
      } else {
        requestResponse = requestService.create(v3Request);
        created = true;
      }
    } else {
      requestResponse = requestService.create(v3Request);
      created = true;
    }
    QueryV2DTO response = modelMapper.map(requestResponse, QueryV2DTO.class);
    if (created) {
      return ResponseEntity.created(URI.create(response.getRedirectUri())).body(response);
    } else {
      return ResponseEntity.accepted().header("Location", response.getRedirectUri()).body(response);
    }
  }
}
