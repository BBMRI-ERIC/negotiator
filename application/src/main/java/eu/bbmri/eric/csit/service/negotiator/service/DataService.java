package eu.bbmri.eric.csit.service.negotiator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.bbmri.eric.csit.service.model.Biobank;
import eu.bbmri.eric.csit.service.model.Collection;
import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.negotiator.dto.request.BiobankDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.CollectionDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.request.QueryRequest;
import eu.bbmri.eric.csit.service.negotiator.dto.response.QueryResponse;
import eu.bbmri.eric.csit.service.repository.BiobankRepository;
import eu.bbmri.eric.csit.service.repository.CollectionRepository;
import eu.bbmri.eric.csit.service.repository.DataSourceRepository;
import eu.bbmri.eric.csit.service.repository.QueryRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DataService {

  private final QueryRepository queryRepository;
  private final CollectionRepository collectionRepository;
  private final BiobankRepository biobankRepository;
  private final DataSourceRepository dataSourceRepository;

  public DataService(
      QueryRepository queryRepository,
      CollectionRepository collectionRepository,
      DataSourceRepository dataSourceRepository,
      BiobankRepository biobankRepository) {
    this.queryRepository = queryRepository;
    this.collectionRepository = collectionRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.biobankRepository = biobankRepository;
  }

  private void checkAndSetResources(Set<BiobankDTO> biobankDTOS, Query queryEntity) {
    Set<Collection> collections = new HashSet<>();
    Set<Biobank> biobanks = new HashSet<>();

    biobankDTOS.forEach(
        biobankDTO -> {
          List<CollectionDTO> collectionDTOs = biobankDTO.getCollections();
          if (collectionDTOs != null) {
            collectionDTOs.forEach(
                collectionDTO -> {
                  Collection collectionEntity =
                      collectionRepository
                          .findBySourceId(collectionDTO.getId())
                          .orElseThrow(
                              () ->
                                  new ResponseStatusException(
                                      HttpStatus.BAD_REQUEST,
                                      String.format(
                                          "Specified collection %s not found",
                                          collectionDTO.getId())));
                  if (!collectionEntity.getBiobank().getSourceId().equals(biobankDTO.getId())) {
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format(
                            "Specified collection %s doesn't belong to the specified biobank %s",
                            collectionDTO.getId(), biobankDTO.getId()));
                  }
                  collections.add(collectionEntity);
                });
          }
        });
    queryEntity.setCollections(collections);
    queryEntity.setBiobanks(biobanks);
  }

  private void checkAndSetDataSource(String url, Query queryEntity) {
    DataSource dataSource =
        dataSourceRepository
            .findByUrl(url)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data source not found"));
    queryEntity.setDataSource(dataSource);
  }

  public QueryResponse createQuery(QueryRequest queryRequest) {
    Query queryEntity = new Query();
    checkAndSetResources(queryRequest.getResources(), queryEntity);
    checkAndSetDataSource(queryRequest.getUrl(), queryEntity);
    queryEntity.setUrl(queryRequest.getUrl());

    ObjectMapper mapper = new ObjectMapper();
    try {
      String jsonPayload = mapper.writeValueAsString(queryRequest);
      queryEntity.setJsonPayload(jsonPayload);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot serialize the request");
    }
    queryRepository.save(queryEntity);

    QueryResponse response = new QueryResponse();
    response.setId(queryEntity.getId());
    response.setUrl(queryRequest.getUrl());
    response.setHumanReadable(queryRequest.getHumanReadable());
    response.setResources(queryRequest.getResources());
    response.setQueryToken(queryEntity.getQueryToken());
    return response;
  }

  public List<Query> findAllQueries() {
    return queryRepository.findAll();
  }

  public Optional<Query> getQueryById(Long id) {
    return queryRepository.findById(id);
  }

  public Collection getCollectionById(Long id) {
    return collectionRepository.getById(id);
  }
}
