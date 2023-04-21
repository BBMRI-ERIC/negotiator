package eu.bbmri.eric.csit.service.negotiator.service;

import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSectionDTO;
import eu.bbmri.eric.csit.service.negotiator.api.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSection;
import eu.bbmri.eric.csit.service.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri.eric.csit.service.negotiator.database.repository.AccessCriteriaSetRepository;
import eu.bbmri.eric.csit.service.negotiator.exceptions.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccessCriteriaSetServiceImpl implements AccessCriteriaSetService {

  private final AccessCriteriaSetRepository accessCriteriaSetRepository;

  private final ModelMapper modelMapper;

  public AccessCriteriaSetServiceImpl(
      AccessCriteriaSetRepository accessCriteriaSetRepository, ModelMapper modelMapper) {
    this.accessCriteriaSetRepository = accessCriteriaSetRepository;
    this.modelMapper = modelMapper;

    TypeMap<AccessCriteriaSet, AccessCriteriaSetDTO> typeMap =
        modelMapper.createTypeMap(AccessCriteriaSet.class, AccessCriteriaSetDTO.class);

    Converter<Set<AccessCriteriaSection>, List<AccessCriteriaSectionDTO>> accessCriteriaConverter =
        ffc -> formFieldConverter(ffc.getSource());
    typeMap.addMappings(
        mapper ->
            mapper
                .using(accessCriteriaConverter)
                .map(AccessCriteriaSet::getSections, AccessCriteriaSetDTO::setSections));
  }

  private List<AccessCriteriaSectionDTO> formFieldConverter(Set<AccessCriteriaSection> sections) {
    return sections.stream()
        .map(
            section -> {
              List<AccessCriteriaDTO> accessCriteria = section.getAccessCriteriaSectionLink()
                  .stream().map(
                      criteria -> new AccessCriteriaDTO(
                          criteria.getAccessCriteria().getName(),
                          criteria.getAccessCriteria().getLabel(),
                          criteria.getAccessCriteria().getDescription(),
                          criteria.getAccessCriteria().getType(),
                          criteria.getRequired())
                  ).toList();
              return new AccessCriteriaSectionDTO(
                  section.getName(),
                  section.getLabel(),
                  section.getDescription(),
                  accessCriteria);
            }
        )
        .collect(Collectors.toList());
  }


  @Transactional
  public AccessCriteriaSetDTO findByResourceEntityId(String resourceEntityId) {
    Optional<AccessCriteriaSet> acs = accessCriteriaSetRepository.findByResourceEntityId(resourceEntityId);
       acs.orElseThrow(() -> new EntityNotFoundException(resourceEntityId));
    return modelMapper.map(acs, AccessCriteriaSetDTO.class);
  }
}
