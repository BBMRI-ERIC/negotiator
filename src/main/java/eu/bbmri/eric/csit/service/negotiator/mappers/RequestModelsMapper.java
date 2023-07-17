package eu.bbmri.eric.csit.service.negotiator.mappers;

import eu.bbmri.eric.csit.service.negotiator.database.model.Negotiation;
import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.dto.request.CollectionV2DTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryCreateV2DTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryV2DTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.RequestDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestModelsMapper {

  @Autowired ModelMapper modelMapper;
  
  private final String FRONTEND_URL;

  public RequestModelsMapper(@Value("${negotiator.frontend-url}") String frontendUrl){
    this.FRONTEND_URL = frontendUrl;
  }
  @PostConstruct
  public void addMappings() {
    TypeMap<Request, RequestDTO> typeMap =
        modelMapper.createTypeMap(Request.class, RequestDTO.class);

    Converter<String, String> requestToRedirectUrl = q -> convertIdToRedirectUrl(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper.using(requestToRedirectUrl).map(Request::getId, RequestDTO::setRedirectUrl));

    Converter<Negotiation, String> negotiationToNegotiationId =
        q -> convertNegotiationToNegotiationId(q.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(negotiationToNegotiationId)
                .map(Request::getNegotiation, RequestDTO::setNegotiationId));

    ///////////////////////////////////////////
    // Mapper from v2 Request to V3 Request
    TypeMap<QueryCreateV2DTO, RequestCreateDTO> v2ToV3Map =
        modelMapper.createTypeMap(QueryCreateV2DTO.class, RequestCreateDTO.class);

    Converter<Set<CollectionV2DTO>, Set<ResourceDTO>> collectionV2ToResourceV3 =
        q -> convertCollectionV2ToResourceV3(q.getSource());

    v2ToV3Map.addMappings(
        mapper ->
            mapper
                .using(collectionV2ToResourceV3)
                .map(QueryCreateV2DTO::getCollections, RequestCreateDTO::setResources));

    // Mapper from v2 Request to V3 Request
    TypeMap<RequestDTO, QueryV2DTO> queryToV3Response =
        modelMapper.createTypeMap(RequestDTO.class, QueryV2DTO.class);

    Converter<RequestDTO, String> queryToRedirectUri = q -> convertIdToRedirectUri(q.getSource());
    queryToV3Response.addMappings(
        mapper ->
            mapper
                .using(queryToRedirectUri)
                .map(requestDTO -> requestDTO, QueryV2DTO::setRedirectUri));

    TypeMap<RequestCreateDTO, Request> requestCreateDTORequestTypeMap =
        modelMapper.createTypeMap(RequestCreateDTO.class, Request.class);
  }

  private String convertNegotiationToNegotiationId(Negotiation negotiation) {
    return negotiation != null ? negotiation.getId() : null;
  }

  private String convertIdToRedirectUrl(String requestId) {
    if (FRONTEND_URL.endsWith("/")){
      return "%srequests/%s".formatted(FRONTEND_URL, requestId);
    }
    return "%s/requests/%s".formatted(FRONTEND_URL, requestId);
  }

  private Set<ResourceDTO> convertCollectionV2ToResourceV3(Set<CollectionV2DTO> collections) {
    Set<ResourceDTO> resourceDTOS = new HashSet<>();
    collections.forEach(
        collection -> {
          ResourceDTO resourceDTO = new ResourceDTO();
          resourceDTO.setId(collection.getCollectionId());
          resourceDTOS.add(resourceDTO);
        });

    return resourceDTOS;
  }

  private String convertIdToRedirectUri(RequestDTO req) {
    if (req.getNegotiationId() == null) {
      return "%s/requests/%s".formatted(FRONTEND_URL, req.getId());
    } else {
      return "%s/negotiations/%s/requests/%s"
          .formatted(FRONTEND_URL, req.getNegotiationId(), req.getId());
    }
  }
}
