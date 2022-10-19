package eu.bbmri.eric.csit.service.negotiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.request.ResourceDTO;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import eu.bbmri.eric.csit.service.negotiator.exceptions.WrongRequestException;
import eu.bbmri.eric.csit.service.negotiator.model.DataSource;
import eu.bbmri.eric.csit.service.negotiator.model.Query;
import eu.bbmri.eric.csit.service.negotiator.model.Resource;
import eu.bbmri.eric.csit.service.negotiator.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.QueryRepository;
import eu.bbmri.eric.csit.service.negotiator.repository.ResourceRepository;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QueryService {

  @Autowired private QueryRepository queryRepository;
  @Autowired private ResourceRepository resourceRepository;
  @Autowired private DataSourceRepository dataSourceRepository;
  @Autowired private ModelMapper modelMapper;

  /**
   * Checks that resources in input conforms to the hierarchy regitered in the negotiator,
   * and if they do, add the leaf resources to the query
   * @param resourceDTOs The List of Resources in the query request
   * @param queryEntity The Query Entity to save in the DB
   */
  private void checkAndSetResources(Set<ResourceDTO> resourceDTOs, Query queryEntity) {
    Set<Resource> resourcesInQuery = new HashSet<>();
    resourceDTOs.forEach(  // For each parent
        resourceDTO -> {
          // Gets the children
          Set<ResourceDTO> childrenDTOs = resourceDTO.getChildren();
          // Gets from the DB all the Resources with the ids of the children and parentId of the
          // parent
          Set<Resource> childrenResources =
              resourceRepository.findBySourceIdInAndParentSourceId(
                  childrenDTOs.stream().map(ResourceDTO::getId).collect(Collectors.toSet()),
                  resourceDTO.getId());
          // If the Resources in the DB are the same as the one in input, it means they are all correct
          if (childrenResources.size() < childrenDTOs.size()) {
            throw new WrongRequestException(
                "Some of the specified resources were not found or the hierarchy was not correct");
          } else {
            resourcesInQuery.addAll(childrenResources);
          }
        }
    );
    queryEntity.setResources(resourcesInQuery);
  }

  /**
   * Checks that the DataSource corresponding to the URL is present in the DB and adds it to the Query entity
   * @param url the url of the DataSource in the incoming query
   * @param queryEntity the Query entity to fill with the DataSource
   */
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
    queryEntity.setHumanReadable(queryRequest.getHumanReadable());
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
  public Query update(String token, QueryRequest queryRequest) {
    Query queryEntity =
        queryRepository.findByToken(token).orElseThrow(() -> new EntityNotFoundException(token));
    return saveQuery(queryRequest, queryEntity);
  }

  @Transactional
  public Query update(Long id, QueryRequest queryRequest) {
    Query queryEntity =
        queryRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(id));
    return saveQuery(queryRequest, queryEntity);
  }
}
