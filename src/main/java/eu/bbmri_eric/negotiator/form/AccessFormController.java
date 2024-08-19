package eu.bbmri_eric.negotiator.form;

import eu.bbmri_eric.negotiator.form.dto.AccessFormCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.AccessFormDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementLinkDTO;
import eu.bbmri_eric.negotiator.form.dto.ElementMetaDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionCreateDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionLinkDTO;
import eu.bbmri_eric.negotiator.form.dto.SectionMetaDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping(value = "/v3", produces = MediaTypes.HAL_JSON_VALUE)
@Tag(name = "Dynamic access forms", description = "Setup and retrieve dynamic access forms")
@SecurityRequirement(name = "security_auth")
public class AccessFormController {

  private final AccessCriteriaSetService accessCriteriaSetService;
  private final AccessFormElementService elementService;
  private final AccessFormsSectionService sectionService;
  private final AccessFormService accessFormService;
  private final ValueSetService valueSetService;
  private final AccessFormModelAssembler accessFormModelAssembler;
  private final AccessFormElementAssembler accessFormElementAssembler;
  private final AccessFormSectionAssembler accessFormSectionAssembler;
  private final ValueSetAssembler valueSetAssembler;

  public AccessFormController(
      AccessCriteriaSetService accessCriteriaSetService,
      AccessFormElementService elementService,
      AccessFormsSectionService sectionService,
      AccessFormService accessFormService,
      AccessFormModelAssembler accessFormModelAssembler,
      AccessFormElementAssembler accessFormElementAssembler,
      AccessFormSectionAssembler accessFormSectionAssembler,
      ValueSetService valueSetService,
      ValueSetAssembler valueSetAssembler) {
    this.accessCriteriaSetService = accessCriteriaSetService;
    this.elementService = elementService;
    this.sectionService = sectionService;
    this.accessFormService = accessFormService;
    this.valueSetService = valueSetService;
    this.accessFormModelAssembler = accessFormModelAssembler;
    this.accessFormElementAssembler = accessFormElementAssembler;
    this.accessFormSectionAssembler = accessFormSectionAssembler;
    this.valueSetAssembler = valueSetAssembler;
  }

  @GetMapping(value = "/access-forms")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get all access forms", description = "List all access forms")
  public PagedModel<EntityModel<AccessFormDTO>> getAllAccessForms(
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestParam(required = false, defaultValue = "50") int size) {
    return accessFormModelAssembler.toPagedModel(
        (Page<AccessFormDTO>) accessFormService.getAllAccessForms(PageRequest.of(page, size)));
  }

  @PostMapping(value = "/access-forms")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new access form")
  public EntityModel<AccessFormDTO> createAccessForm(
      @Valid @RequestBody AccessFormCreateDTO createDTO) {
    return accessFormModelAssembler.toModel(accessFormService.createAccessForm(createDTO));
  }

  @GetMapping(value = "/access-forms/{formId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an access form by id", description = "Returns an access form by id")
  public EntityModel<AccessFormDTO> getAccessFormById(@PathVariable Long formId) {
    return accessFormModelAssembler.toModel(accessFormService.getAccessForm(formId));
  }

  @PutMapping(value = "/access-forms/{formId}/sections")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Link a section to an access form")
  public EntityModel<AccessFormDTO> linkSection(
      @Valid @PathVariable Long formId, @Valid @RequestBody SectionLinkDTO createDTO) {
    return accessFormModelAssembler.toModel(accessFormService.addSection(createDTO, formId));
  }

  @DeleteMapping(value = "/access-forms/{formId}/sections/{sectionId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Unlink an element from a specific section in an access form ")
  public EntityModel<AccessFormDTO> unlinkSection(
      @Valid @PathVariable Long formId, @Valid @PathVariable Long sectionId) {
    return accessFormModelAssembler.toModel(accessFormService.removeSection(formId, sectionId));
  }

  @PutMapping(value = "/access-forms/{formId}/sections/{sectionId}/elements")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Link an element to a specific section in an access form ")
  public EntityModel<AccessFormDTO> linkElement(
      @Valid @PathVariable Long formId,
      @Valid @PathVariable Long sectionId,
      @Valid @RequestBody ElementLinkDTO createDTO) {
    return accessFormModelAssembler.toModel(
        accessFormService.addElement(createDTO, formId, sectionId));
  }

  @DeleteMapping(value = "/access-forms/{formId}/sections/{sectionId}/elements/{elementId}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Unlink an element from a specific section in an access form ")
  public EntityModel<AccessFormDTO> unlinkElementFromSection(
      @Valid @PathVariable Long formId,
      @Valid @PathVariable Long sectionId,
      @Valid @PathVariable Long elementId) {
    return accessFormModelAssembler.toModel(
        accessFormService.removeElement(formId, sectionId, elementId));
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

  @GetMapping(value = "/elements")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available elements")
  public CollectionModel<EntityModel<ElementMetaDTO>> getAllElements() {
    return accessFormElementAssembler.toCollectionModel(elementService.getAllElements());
  }

  @GetMapping(value = "/elements/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get an element by id", description = "Returns an element by id")
  public EntityModel<ElementMetaDTO> getElementById(@PathVariable Long id) {
    return accessFormElementAssembler.toModel(elementService.getElementById(id));
  }

  @PostMapping(value = "/elements", produces = MediaTypes.HAL_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new element")
  public EntityModel<ElementMetaDTO> createElement(
      @RequestBody @Valid ElementCreateDTO elementCreateDTO) {
    return accessFormElementAssembler.toModel(elementService.createElement(elementCreateDTO));
  }

  @PutMapping(value = "/elements/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update an existing element")
  public EntityModel<ElementMetaDTO> updateElement(
      @RequestBody @Valid ElementCreateDTO dto, @PathVariable Long id) {
    return accessFormElementAssembler.toModel(elementService.updateElement(dto, id));
  }

  @GetMapping(value = "/sections")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available sections")
  public CollectionModel<EntityModel<SectionMetaDTO>> getAllSections() {
    return accessFormSectionAssembler.toCollectionModel(sectionService.getAllSections());
  }

  @GetMapping(value = "/sections/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get a section by id")
  public EntityModel<SectionMetaDTO> getSectionById(@PathVariable Long id) {
    return accessFormSectionAssembler.toModel(sectionService.getSectionById(id));
  }

  @PostMapping(value = "/sections")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new section")
  public EntityModel<SectionMetaDTO> createSection(
      @RequestBody @Valid SectionCreateDTO elementCreateDTO) {
    return accessFormSectionAssembler.toModel(sectionService.createSection(elementCreateDTO));
  }

  @PutMapping(value = "/sections/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update an existing section")
  public EntityModel<SectionMetaDTO> updateSection(
      @RequestBody @Valid SectionCreateDTO dto, @PathVariable Long id) {
    return accessFormSectionAssembler.toModel(sectionService.updateSection(dto, id));
  }

  @GetMapping(value = "/value-sets")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "List all available value sets")
  public CollectionModel<EntityModel<ValueSetDTO>> getAllValueSets() {
    return valueSetAssembler.toCollectionModel(valueSetService.getAllValueSets());
  }

  @GetMapping(value = "/value-sets/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Get a value set by id")
  public EntityModel<ValueSetDTO> getValueSetById(@PathVariable Long id) {
    return valueSetAssembler.toModel(valueSetService.getValueSetById(id));
  }

  @PostMapping(value = "/value-sets")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new ValueSet")
  public EntityModel<ValueSetDTO> createValueSet(@RequestBody @Valid ValueSetCreateDTO createDTO) {
    return valueSetAssembler.toModel(valueSetService.createValueSet(createDTO));
  }

  @PutMapping(value = "/value-sets/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Update an existing ValueSet")
  public EntityModel<ValueSetDTO> updateValueSet(
      @RequestBody @Valid ValueSetCreateDTO dto, @PathVariable Long id) {
    return valueSetAssembler.toModel(valueSetService.updateValueSet(dto, id));
  }
}
