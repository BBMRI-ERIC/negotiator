package eu.bbmri_eric.negotiator.form.service;

import eu.bbmri_eric.negotiator.common.exceptions.EntityNotFoundException;
import eu.bbmri_eric.negotiator.form.AccessFormSection;
import eu.bbmri_eric.negotiator.form.dto.SectionCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionMetaDTO;
import eu.bbmri_eric.negotiator.form.repository.AccessFormSectionRepository;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class AccessFormsSectionServiceImpl implements AccessFormsSectionService {

  private final AccessFormSectionRepository repository;
  private final ModelMapper mapper;

  public AccessFormsSectionServiceImpl(AccessFormSectionRepository repository, ModelMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public List<SectionMetaDTO> getAllSections() {
    return repository.findAll(Sort.by("id").ascending()).stream()
        .map((element) -> mapper.map(element, SectionMetaDTO.class))
        .toList();
  }

  @Override
  public SectionMetaDTO getSectionById(Long id) {
    return mapper.map(
        repository.findById(id).orElseThrow(() -> new EntityNotFoundException(id)),
        SectionMetaDTO.class);
  }

  @Override
  public SectionMetaDTO createSection(SectionCreateDTO sectionCreateDTO) {
    AccessFormSection section = mapper.map(sectionCreateDTO, AccessFormSection.class);
    return mapper.map(repository.save(section), SectionMetaDTO.class);
  }

  @Override
  public SectionMetaDTO updateSection(SectionCreateDTO sectionCreateDTO, Long id) {
    AccessFormSection section = mapper.map(sectionCreateDTO, AccessFormSection.class);
    section.setId(id);
    return mapper.map(repository.save(section), SectionMetaDTO.class);
  }
}
