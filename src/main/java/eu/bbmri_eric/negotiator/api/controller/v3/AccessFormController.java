package eu.bbmri_eric.negotiator.api.controller.v3;

import eu.bbmri_eric.negotiator.dto.access_form.AccessFormDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.ElementMetaDTO;
import eu.bbmri_eric.negotiator.dto.access_form.SectionCreateDTO;
import eu.bbmri_eric.negotiator.dto.access_form.SectionMetaDTO;
import eu.bbmri_eric.negotiator.mappers.AccessFormElementAssembler;
import eu.bbmri_eric.negotiator.mappers.AccessFormModelAssembler;
import eu.bbmri_eric.negotiator.mappers.AccessFormSectionAssembler;
import eu.bbmri_eric.negotiator.service.AccessCriteriaSetService;
import eu.bbmri_eric.negotiator.service.AccessFormElementService;
import eu.bbmri_eric.negotiator.service.AccessFormService;
import eu.bbmri_eric.negotiator.service.AccessFormsSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(
    value = "/v3",
    produces = MediaTypes.HAL_JSON_VALUE,
    consumes = MediaType.APPLICATION_JSON_VALUE)
@CrossOrigin
@Tag(name = "Dynamic access forms", description = "Setup and retrieve dynamic access forms")
public class AccessFormController {

  private final AccessCriteriaSetService accessCriteriaSetService;
  private final AccessFormElementService elementService;
  private final AccessFormsSectionService sectionService;
  private final AccessFormService accessFormService;
  private final AccessFormModelAssembler accessFormModelAssembler;
  private final ModelMapper modelMapper;
  private final AccessFormElementAssembler accessFormElementAssembler;
  private final AccessFormSectionAssembler accessFormSectionAssembler;

  public AccessFormController(
      AccessCriteriaSetService accessCriteriaSetService,
      AccessFormElementService elementService,
      AccessFormsSectionService sectionService,
      AccessFormService accessFormService,
      AccessFormModelAssembler accessFormModelAssembler,
      ModelMapper modelMapper,
      AccessFormElementAssembler accessFormElementAssembler,
      AccessFormSectionAssembler accessFormSectionAssembler) {
    this.accessCriteriaSetService = accessCriteriaSetService;
    this.elementService = elementService;
    this.sectionService = sectionService;
    this.accessFormService = accessFormService;
    this.accessFormModelAssembler = accessFormModelAssembler;
    this.modelMapper = modelMapper;
    this.accessFormElementAssembler = accessFormElementAssembler;
    this.accessFormSectionAssembler = accessFormSectionAssembler;
  }

  @GetMapping(value = "/access-criteria")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Search access criteria",
      description = "Search access criteria by resource id",
      deprecated = true)
  EntityModel<AccessFormDTO> search(@RequestParam String resourceId) {
    return accessFormModelAssembler.toModel(accessCriteriaSetService.findByResourceId(resourceId));
  }

  @GetMapping(value = "/requests/{id}/access-form")
  @ResponseStatus(HttpStatus.OK)
  @Operation(
      summary = "Get an access form for a request",
      description =
          "Returns an access form with sections and"
              + " elements that are relevant for the given resources being requested.")
  public EntityModel<AccessFormDTO> combine(@PathVariable String id) {
    return accessFormModelAssembler.toModel(accessFormService.getAccessFormForRequest(id));
  }

  @GetMapping(value = "/access-forms/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an access form by id", description = "Returns an access form by id")
  public EntityModel<AccessFormDTO> findById(@PathVariable Long id) {
    return accessFormModelAssembler.toModel(accessFormService.getAccessForm(id));
  }

  @GetMapping(value = "/access-forms")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get all access forms", description = "List all access forms")
  public PagedModel<EntityModel<AccessFormDTO>> list(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return accessFormModelAssembler.toPagedModel(
        (Page<AccessFormDTO>) accessFormService.getAllAccessForms(PageRequest.of(page, size)));
  }

  @GetMapping(value = "/elements")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available elements")
  public CollectionModel<EntityModel<ElementMetaDTO>> getAll() {
    return accessFormElementAssembler.toCollectionModel(elementService.getAll());
  }

  @GetMapping(value = "/elements/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an element by id", description = "Returns an element by id")
  public EntityModel<ElementMetaDTO> getElementById(@PathVariable Long id) {
    return accessFormElementAssembler.toModel(elementService.getById(id));
  }

  @PostMapping(value = "/elements")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new element")
  public EntityModel<ElementMetaDTO> createElement(
      @RequestBody @Valid ElementCreateDTO elementCreateDTO) {
    return accessFormElementAssembler.toModel(elementService.create(elementCreateDTO));
  }

  @PutMapping(value = "/elements/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update an existing element")
  public EntityModel<ElementMetaDTO> updateElement(
      @RequestBody @Valid ElementCreateDTO dto, @PathVariable Long id) {
    return accessFormElementAssembler.toModel(elementService.update(dto, id));
  }

  @GetMapping(value = "/sections", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available sections")
  public CollectionModel<EntityModel<SectionMetaDTO>> getAllSections() {
    return accessFormSectionAssembler.toCollectionModel(sectionService.getAllSections());
  }

  @GetMapping(value = "/sections/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get a section by id")
  public EntityModel<SectionMetaDTO> getSectionById(@PathVariable Long id) {
    return accessFormSectionAssembler.toModel(sectionService.getSectionById(id));
  }

  @PostMapping(value = "/sections", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new section")
  public EntityModel<SectionMetaDTO> createSection(
      @RequestBody @Valid SectionCreateDTO elementCreateDTO) {
    return accessFormSectionAssembler.toModel(sectionService.createSection(elementCreateDTO));
  }

  @PutMapping(value = "/section/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update an existing section")
  public EntityModel<SectionMetaDTO> updateSection(
      @RequestBody @Valid SectionCreateDTO dto, @PathVariable Long id) {
    return accessFormSectionAssembler.toModel(sectionService.updateSection(dto, id));
  }
}
