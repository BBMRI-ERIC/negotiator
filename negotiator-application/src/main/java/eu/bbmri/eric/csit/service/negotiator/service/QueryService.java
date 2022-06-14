package eu.bbmri.eric.csit.service.negotiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.negotiator.model.Collection;
import eu.bbmri.eric.csit.service.negotiator.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.repository.CollectionRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.QueryRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

          Set<Collection> newCollections =
              collectionRepository.findBySourceIdInAndBiobankSourceId(
                  childrenDTOs.stream().map(ResourceDTO::getId).collect(Collectors.toSet()),
                  resourceDTO.getId());

          if (newCollections.size() < childrenDTOs.size()) {
            throw new WrongRequestException(
                "Some of the specified resources were not found or the hierarchy was not correct");
          } else {
            collections.addAll(newCollections);
          }
        });
    queryEntity.setCollections(collections);
  }

  private void checkAndSetDataSource(String url, Query queryEntity) {
    URL dataSourceURL;
    try {
      dataSourceURL = new URL(url);
    } catch (MalformedURLException e) {
      throw new WrongRequestException("URL not valid");
    }
    DataSource dataSource =
        dataSourceRepository
            .findByUrl(
                String.format("%s://%s", dataSourceURL.getProtocol(), dataSourceURL.getHost()))
            .orElseThrow(() -> new WrongRequestException("Data source not found"));
    queryEntity.setDataSource(dataSource);
  }

  private Query saveQuery(QueryRequest queryRequest, Query queryEntity) {
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

  @Transactional
  public Query create(QueryRequest queryRequest) {
    Query queryEntity = new Query();
    return saveQuery(queryRequest, queryEntity);
  }

  @Transactional(readOnly = true)
  public List<Query> findAll() {
    return queryRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Query findById(Long id) {
    return queryRepository.findDetailedById(id).orElseThrow(() -> new EntityNotFoundException(id));
  }

  public Set<Query> findAllById(Set<Long> ids) {
    return ids.stream().map(this::findById).collect(Collectors.toSet());
  }

  @Transactional
  public Query update(Long id, QueryRequest queryRequest) {
    Query queryEntity =
        queryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return saveQuery(queryRequest, queryEntity);
  }
}
