package eu.bbmri.eric.csit.service.negotiator.api.v3;

import eu.bbmri.eric.csit.service.model.Biobank;
import eu.bbmri.eric.csit.service.model.Collection;
import eu.bbmri.eric.csit.service.repository.BiobankRepository;
import eu.bbmri.eric.csit.service.repository.CollectionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/v3")
public class CollectionController {

  private final CollectionRepository collectionRepository;
  private final BiobankRepository biobankRepository;

  CollectionController(
      CollectionRepository collectionRepository, BiobankRepository biobankRepository) {
    this.collectionRepository = collectionRepository;
    this.biobankRepository = biobankRepository;
  }

  @GetMapping(value = "/collections/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  Collection retrieve(@PathVariable Long id) {
    return collectionRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
  }

  @GetMapping(value = "/biobanks/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  Biobank retrieveBiobank(@PathVariable Long id) {
    return biobankRepository
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Biobank not found"));
  }
}
