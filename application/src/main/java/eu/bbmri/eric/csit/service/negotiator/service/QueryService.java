package eu.bbmri.eric.csit.service.negotiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.bbmri.eric.csit.service.model.Collection;
import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.repository.CollectionRepository;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.repository.QueryRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
public class QueryService {

  private final QueryRepository queryRepository;
  private final CollectionRepository collectionRepository;
  private final DataSourceRepository dataSourceRepository;

  public QueryService(
      QueryRepository queryRepository,
      CollectionRepository collectionRepository,
      DataSourceRepository dataSourceRepository) {
    this.queryRepository = queryRepository;
    this.collectionRepository = collectionRepository;
    this.dataSourceRepository = dataSourceRepository;
  }

  private void checkAndSetResources(Set<ResourceDTO> resourceDTOs, Query queryEntity) {
    Set<Collection> collections = new HashSet<>();
    // Currently, we assume the biobank -> collection hierarchy
    resourceDTOs.forEach(
        resourceDTO -> {
          Set<ResourceDTO> childrenDTOs = resourceDTO.getChildren();
          if (childrenDTOs != null) {
            childrenDTOs.forEach(
                childrenDTO -> {
                  Collection collectionEntity =
                      collectionRepository
                          .findBySourceId(childrenDTO.getId())
                          .orElseThrow(
                              () ->
                                  new WrongRequestException(
                                      String.format(
                                          "Collection %s not found", childrenDTO.getId())));

                  if (!collectionEntity.getBiobank().getSourceId().equals(resourceDTO.getId())) {
                    throw new WrongRequestException(
                        String.format(
                            "Collection %s doesn't belong to biobank %s",
                            childrenDTO.getId(), resourceDTO.getId()));
                  }
                  collections.add(collectionEntity);
                });
          }
        });
    queryEntity.setCollections(collections);
  }

  private void checkAndSetDataSource(String url, Query queryEntity) {
    DataSource dataSource =
        dataSourceRepository
            .findByUrl(url)
            .orElseThrow(() -> new WrongRequestException("Data source not found"));
    queryEntity.setDataSource(dataSource);
  }

  public Query create(QueryRequest queryRequest) {
    Query queryEntity = new Query();
    checkAndSetResources(queryRequest.getResources(), queryEntity);
    checkAndSetDataSource(queryRequest.getUrl(), queryEntity);
    queryEntity.setUrl(queryRequest.getUrl());

    JsonMapper mapper = new JsonMapper();
    try {
      String jsonPayload = mapper.writeValueAsString(queryRequest);
      queryEntity.setJsonPayload(jsonPayload);
    } catch (JsonProcessingException e) {
      throw new WrongRequestException();
    }
    return queryRepository.save(queryEntity);
  }

  public List<Query> findAll() {
    return queryRepository.findAll();
  }

  public Query getById(Long id) {
    return queryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }
}
