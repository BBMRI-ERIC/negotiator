package eu.bbmri_eric.negotiator.negotiation.request;

import eu.bbmri_eric.negotiator.negotiation.NegotiationService;
import eu.bbmri_eric.negotiator.negotiation.dto.QueryCreateV2DTO;
import eu.bbmri_eric.negotiator.negotiation.dto.QueryV2DTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestCreateDTO;
import eu.bbmri_eric.negotiator.negotiation.dto.RequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Queries", description = "Deprecated! Replaced by Requests")
public class QueryV2Controller {

  @Autowired private RequestService requestService;
  @Autowired private NegotiationService negotiationService;
  @Autowired private ModelMapper modelMapper;

  @PostMapping(
      value = "/directory/create_query",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      summary = "Create a new request",
      description = "Create a new request",
      deprecated = true)
  ResponseEntity<QueryV2DTO> add(@Valid @RequestBody QueryCreateV2DTO queryRequest) {
    RequestCreateDTO v3Request = modelMapper.map(queryRequest, RequestCreateDTO.class);
    RequestDTO requestResponse;
    requestResponse = requestService.create(v3Request);
    QueryV2DTO response = modelMapper.map(requestResponse, QueryV2DTO.class);
    return ResponseEntity.created(URI.create(response.getRedirectUri())).body(response);
  }
}
