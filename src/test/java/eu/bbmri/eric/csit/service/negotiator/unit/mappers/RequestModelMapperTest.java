package eu.bbmri.eric.csit.service.negotiator.unit.mappers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri.eric.csit.service.negotiator.database.model.Request;
import eu.bbmri.eric.csit.service.negotiator.database.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.dto.request.*;
import eu.bbmri.eric.csit.service.negotiator.mappers.RequestModelsMapper;
import eu.bbmri.eric.csit.service.negotiator.mappers.ResourceModelMapper;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.modelmapper.ModelMapper;

public class RequestModelMapperTest {
  @Spy public ModelMapper mapper = new ModelMapper();

  @InjectMocks RequestModelsMapper requestModelsMapper;

  @InjectMocks ResourceModelMapper resourceModelMapper;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);
    this.requestModelsMapper.addMappings();
    this.resourceModelMapper.addMappings();
  }

  @Test
  void map_requestToDTO_Ok() {
    Request request = new Request();
    request.setId("newRequest");
    Resource resource = new Resource();
    resource.setSourceId("collection:1");
    request.setResources(Set.of(resource));
    assertEquals(request.getId(), this.mapper.map(request, RequestDTO.class).getId());
    assertEquals(1, this.mapper.map(request, RequestDTO.class).getResources().size());
    assertEquals(
        resource.getSourceId(),
        this.mapper.map(request, RequestDTO.class).getResources().iterator().next().getId());
  }

  @Test
  void map_createDTOtoRequest_Ok() {
    ResourceDTO resourceDTO =
        ResourceDTO.builder().id("test:collection").name("My collection").build();
    RequestCreateDTO requestCreateDTO =
        RequestCreateDTO.builder()
            .url("https://directory.com")
            .humanReadable("I want everything")
            .resources(Set.of(resourceDTO))
            .build();
    Request request = this.mapper.map(requestCreateDTO, Request.class);
    assertEquals(requestCreateDTO.getHumanReadable(), request.getHumanReadable());
    assertEquals(requestCreateDTO.getUrl(), request.getUrl());
    assertEquals(
        requestCreateDTO.getResources().iterator().next().getId(),
        request.getResources().iterator().next().getSourceId());
  }

  @Test
  void map_v2QueryToRequestCreateDTO_ok() {
    QueryCreateV2DTO queryCreateV2DTO = buildQueryCreateV2DTO();
    RequestCreateDTO requestCreateDTO = mapper.map(queryCreateV2DTO, RequestCreateDTO.class);
    assertEquals(queryCreateV2DTO.getHumanReadable(), requestCreateDTO.getHumanReadable());
    assertEquals(queryCreateV2DTO.getUrl(), requestCreateDTO.getUrl());
    queryCreateV2DTO.getCollections().forEach(
            collectionV2DTO -> assertTrue(requestCreateDTO.getResources().stream()
                    .anyMatch(collection -> Objects.equals(collection.getId(),
                            collectionV2DTO.getCollectionId())))
    );
    
  }

  private static QueryCreateV2DTO buildQueryCreateV2DTO() {
    Set<CollectionV2DTO> collectionV2DTOS = new HashSet<>();
    collectionV2DTOS.add(CollectionV2DTO.builder().
            collectionId("collection1")
                    .biobankId("biobank1")
            .build());
    collectionV2DTOS.add(CollectionV2DTO.builder().
            collectionId("collection2")
            .biobankId("biobank2")
            .build());
    return QueryCreateV2DTO.builder()
            .url("https://directory.com")
            .humanReadable("I want everything!")
            .token("randomlyGeneratedString")
            .collections(collectionV2DTOS)
            .build();
  }
}
