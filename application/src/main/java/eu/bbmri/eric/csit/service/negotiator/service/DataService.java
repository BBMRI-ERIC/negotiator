package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.model.Biobank;
import eu.bbmri.eric.csit.service.model.Collection;
import eu.bbmri.eric.csit.service.model.DataSource;
import eu.bbmri.eric.csit.service.model.Query;
import eu.bbmri.eric.csit.service.negotiator.dto.CollectionDTO;
import eu.bbmri.eric.csit.service.negotiator.dto.QueryDTO;
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
  private final DataSourceRepository dataSourceRepository;

  public DataService(
      QueryRepository queryRepository,
      CollectionRepository collectionRepository,
      DataSourceRepository dataSourceRepository) {
    this.queryRepository = queryRepository;
    this.collectionRepository = collectionRepository;
    this.dataSourceRepository = dataSourceRepository;
  }

  private void checkAndSetBiobanksAndCollections(
      List<CollectionDTO> collectionDTOs, Query queryEntity) {
    Set<Collection> collections = new HashSet<>();
    Set<Biobank> biobanks = new HashSet<>();

    collectionDTOs.forEach(
        collectionDTO -> {
          Collection collectionEntity =
              collectionRepository
                  .findBySourceId(collectionDTO.getCollectionId())
                  .orElseThrow(
                      () ->
                          new ResponseStatusException(
                              HttpStatus.BAD_REQUEST,
                              String.format(
                                  "Specified collection %s not found",
                                  collectionDTO.getCollectionId())));
          if (!collectionEntity.getBiobank().getSourceId().equals(collectionDTO.getBiobankId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                String.format(
                    "Specified collection %s doesn't belong to the specified biobank %s",
                    collectionDTO.getCollectionId(), collectionDTO.getBiobankId()));
          }
          collections.add(collectionEntity);
          biobanks.add(collectionEntity.getBiobank());
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

  public Query createQuery(QueryDTO query) {
    Query queryEntity = new Query();
    checkAndSetBiobanksAndCollections(query.getCollections(), queryEntity);
    checkAndSetDataSource(query.getUrl(), queryEntity);
    queryEntity.setJsonPayload("\"test\"");
    queryRepository.save(queryEntity);
    return queryEntity;
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
