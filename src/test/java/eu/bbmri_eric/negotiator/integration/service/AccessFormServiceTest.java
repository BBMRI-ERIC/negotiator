package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.bbmri_eric.negotiator.database.model.AccessForm;
import eu.bbmri_eric.negotiator.database.model.AccessFormElement;
import eu.bbmri_eric.negotiator.database.model.AccessFormSection;
import eu.bbmri_eric.negotiator.database.model.DataSource;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessFormElementRepository;
import eu.bbmri_eric.negotiator.database.repository.AccessFormRepository;
import eu.bbmri_eric.negotiator.database.repository.AccessFormSectionRepository;
import eu.bbmri_eric.negotiator.database.repository.DataSourceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSectionDTO;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestDTO;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.service.AccessCriteriaSetService;
import eu.bbmri_eric.negotiator.service.AccessFormService;
import eu.bbmri_eric.negotiator.service.RequestService;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AccessFormServiceTest {

  @Autowired AccessFormService accessFormService;

  @Autowired RequestService requestService;

  @Autowired RequestRepository requestRepository;

  @Autowired AccessCriteriaSetService accessCriteriaSetService;

  @Autowired ResourceRepository resourceRepository;

  @Autowired AccessFormRepository accessFormRepository;
  @Autowired AccessFormSectionRepository accessFormSectionRepository;
  @Autowired AccessFormElementRepository accessFormElementRepository;

  @Autowired DataSourceRepository dataSourceRepository;

  @Autowired OrganizationRepository organizationRepository;

  @Test
  void getAccessFormForRequest_nullId_throwsEntityNotFound() {
    assertThrows(
        IllegalArgumentException.class, () -> accessFormService.getAccessFormForRequest(null));
  }

  @Test
  @Transactional
  void getAccessFormForRequest_1resource_identicalFormToResource() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    assertEquals(1, requestDTO.getResources().size());
    AccessCriteriaSetDTO resourceForm =
        accessCriteriaSetService.findByResourceId(
            requestDTO.getResources().iterator().next().getId());
    AccessCriteriaSetDTO requestForm =
        accessFormService.getAccessFormForRequest(requestDTO.getId());
    assertEquals(3, accessFormRepository.findAll().get(0).getLinkedSections().size());
    assertEquals(resourceForm.getSections().size(), requestForm.getSections().size());
    assertEquals(resourceForm, requestForm);
  }

  @Test
  @Transactional
  void getAccessFormForRequest_5resourceWithIdenticalForm_identicalForm() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Request request = requestRepository.findById(requestDTO.getId()).get();
    AccessForm accessForm = accessFormRepository.findAll().get(0);
    request = addResourcesToRequest(accessForm, request);
    assertEquals(5, request.getResources().size());
    for (Resource resource : request.getResources()) {
      assertEquals(
          accessCriteriaSetService.findByResourceId(resource.getSourceId()),
          accessFormService.getAccessFormForRequest(request.getId()));
    }
  }

  @Test
  @Transactional
  void getAccessFormForRequest_2resourcesWithDifferentSections_combined() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Resource resource = resourceRepository.findAll().get(1);
    assertFalse(
        requestDTO.getResources().stream()
            .anyMatch(res -> res.getId().equals(resource.getSourceId())));
    AccessFormSection accessFormSection =
        new AccessFormSection("different_section", "test", "test");
    accessFormSection.setId(100L);
    accessFormSection = accessFormSectionRepository.save(accessFormSection);
    AccessForm accessForm = new AccessForm("different_form");
    accessForm.linkSection(accessFormSection, 0);
    accessForm = accessFormRepository.save(accessForm);
    resource.setAccessForm(accessForm);
    resourceRepository.save(resource);
    Request request = requestRepository.findById(requestDTO.getId()).get();
    request.getResources().add(resource);
    request = requestRepository.save(request);
    assertEquals(2, request.getResources().size());
    assertEquals(
        4, accessFormService.getAccessFormForRequest(request.getId()).getSections().size());
  }

  @Test
  @Transactional
  void getAccessFormForRequest_4DifferentSections1Same_has5() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Resource resource = resourceRepository.findAll().get(1);
    assertFalse(
        requestDTO.getResources().stream()
            .anyMatch(res -> res.getId().equals(resource.getSourceId())));
    AccessFormSection accessFormSection =
        new AccessFormSection("different_section", "test", "test");
    AccessFormSection accessFormSection2 =
        new AccessFormSection("different_section2", "test", "test");
    AccessFormSection sameAccessFormSection = accessFormSectionRepository.findAll().get(0);
    accessFormSection = accessFormSectionRepository.save(accessFormSection);
    accessFormSection2 = accessFormSectionRepository.save(accessFormSection2);
    AccessForm newAccessForm = new AccessForm("different_form");
    newAccessForm.linkSection(accessFormSection, 0);
    newAccessForm.linkSection(accessFormSection2, 1);
    newAccessForm.linkSection(sameAccessFormSection, 2);
    newAccessForm = accessFormRepository.save(newAccessForm);
    resource.setAccessForm(newAccessForm);
    resourceRepository.save(resource);
    Request request = requestRepository.findById(requestDTO.getId()).get();
    request.getResources().add(resource);
    request = requestRepository.save(request);
    assertEquals(2, request.getResources().size());
    assertEquals(
        5, accessFormService.getAccessFormForRequest(request.getId()).getSections().size());
  }

  @Test
  @Transactional
  void getAccessFormForRequest_sameSectionDifferentElements_appendedInOrder() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Resource resource = resourceRepository.findAll().get(1);
    assertFalse(
        requestDTO.getResources().stream()
            .anyMatch(res -> res.getId().equals(resource.getSourceId())));
    AccessForm accessForm = resource.getAccessForm();
    AccessFormElement newElement =
        new AccessFormElement("different_element", "test", "test", "test");
    newElement = accessFormElementRepository.save(newElement);
    AccessForm newAccessForm = new AccessForm("different_form");
    AccessFormSection sameSection =
        accessForm.getLinkedSections().stream()
            .filter(accessFormSection -> accessFormSection.getName().equals("project"))
            .findFirst()
            .get();
    newAccessForm.linkSection(sameSection, 1);
    newAccessForm = accessFormRepository.save(newAccessForm);
    assertFalse(newAccessForm.getLinkedSections().isEmpty());
    newAccessForm.linkElementToSection(
        newAccessForm.getLinkedSections().iterator().next(), newElement, 5, true);
    assertFalse(newAccessForm.getLinkedSections().isEmpty());
    assertTrue(
        newAccessForm.getLinkedSections().stream()
            .iterator()
            .next()
            .getAccessFormElements()
            .stream()
            .anyMatch(
                accessFormElement -> accessFormElement.getName().equals("different_element")));
    newAccessForm = accessFormRepository.save(newAccessForm);
    assertFalse(newAccessForm.getLinkedSections().isEmpty());
    resource.setAccessForm(newAccessForm);
    resourceRepository.save(resource);
    Request request = requestRepository.findById(requestDTO.getId()).get();
    request.getResources().add(resource);
    request = requestRepository.save(request);
    assertEquals(2, accessFormRepository.findAll().size());
    AccessCriteriaSetDTO accessCriteriaSetDTO =
        accessFormService.getAccessFormForRequest(request.getId());
    Optional<AccessCriteriaSectionDTO> section =
        accessCriteriaSetDTO.getSections().stream()
            .filter(
                accessCriteriaSectionDTO ->
                    accessCriteriaSectionDTO.getName().equals(sameSection.getName()))
            .findFirst();
    assertTrue(section.isPresent());
    assertEquals(
        sameSection.getAccessFormElements().size(), section.get().getAccessCriteria().size());
    assertTrue(
        section.get().getAccessCriteria().stream()
            .anyMatch(
                accessCriteriaElementDTO ->
                    accessCriteriaElementDTO.getName().equals("different_element")));
  }

  @Test
  @Transactional
  void getAccessFormForRequest_sameSection1differentElement_combinedTo3() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Resource originalResource =
        resourceRepository
            .findBySourceId(requestDTO.getResources().iterator().next().getId())
            .get();
    Resource resource = resourceRepository.findAll().get(1);
    AccessFormElement newElement =
        new AccessFormElement("different_element", "test", "test", "test");
    newElement = accessFormElementRepository.save(newElement);
    AccessForm newAccessForm = new AccessForm("different_form");
    AccessFormSection sameSection = accessFormSectionRepository.findById(1L).get();
    assertEquals("project", sameSection.getName());
    newAccessForm.linkSection(sameSection, 1);
    newAccessForm = accessFormRepository.save(newAccessForm);
    assertFalse(newAccessForm.getLinkedSections().isEmpty());
    newAccessForm.linkElementToSection(sameSection, newElement, 0, true);
    assertFalse(newAccessForm.getLinkedSections().isEmpty());
    assertTrue(
        newAccessForm.getLinkedSections().stream()
            .iterator()
            .next()
            .getAccessFormElements()
            .stream()
            .anyMatch(
                accessFormElement -> accessFormElement.getName().equals("different_element")));
    newAccessForm = accessFormRepository.save(newAccessForm);
    assertFalse(newAccessForm.getLinkedSections().isEmpty());
    resource.setAccessForm(newAccessForm);
    resource = resourceRepository.save(resource);
    assertEquals(
        1,
        resource.getAccessForm().getLinkedSections().stream()
            .filter(accessFormSection -> accessFormSection.getName().equals(sameSection.getName()))
            .findFirst()
            .get()
            .getAccessFormElements()
            .size());
    assertEquals(
        2,
        originalResource.getAccessForm().getLinkedSections().stream()
            .filter(accessFormSection -> accessFormSection.getName().equals(sameSection.getName()))
            .findFirst()
            .get()
            .getAccessFormElements()
            .size());
    Request request = requestRepository.findById(requestDTO.getId()).get();
    request.getResources().add(resource);
    request = requestRepository.save(request);
    assertEquals(2, accessFormRepository.findAll().size());
    AccessCriteriaSetDTO accessCriteriaSetDTO =
        accessFormService.getAccessFormForRequest(request.getId());
    Optional<AccessCriteriaSectionDTO> section =
        accessCriteriaSetDTO.getSections().stream()
            .filter(
                accessCriteriaSectionDTO ->
                    accessCriteriaSectionDTO.getName().equals(sameSection.getName()))
            .findFirst();
    assertTrue(section.isPresent());
    assertEquals(
        sameSection.getAccessFormElements().size(), section.get().getAccessCriteria().size());
    assertTrue(
        section.get().getAccessCriteria().stream()
            .anyMatch(
                accessCriteriaElementDTO ->
                    accessCriteriaElementDTO.getName().equals("different_element")));
    assertTrue(section.get().getAccessCriteria().size() > 1);
    assertTrue(
        section.get().getAccessCriteria().stream()
            .anyMatch(
                accessCriteriaElementDTO -> accessCriteriaElementDTO.getName().equals("title")));
  }

  private Request addResourcesToRequest(AccessForm accessForm, Request request) {
    Organization organization =
        organizationRepository.save(
            Organization.builder().name("test").externalId("biobank:99").build());
    DataSource dataSource =
        dataSourceRepository.save(
            DataSource.builder()
                .sourcePrefix("")
                .apiPassword("")
                .apiType(DataSource.ApiType.MOLGENIS)
                .apiUrl("")
                .apiUsername("")
                .url("")
                .resourceBiobank("")
                .resourceCollection("")
                .resourceNetwork("")
                .name("")
                .syncActive(true)
                .build());
    for (int i = 0; i < 4; i++) {
      Resource resource =
          resourceRepository.save(
              Resource.builder()
                  .organization(organization)
                  .accessForm(accessForm)
                  .dataSource(dataSource)
                  .sourceId("collection:%s".formatted(i))
                  .name("test")
                  .build());
      request.getResources().add(resource);
    }
    request = requestRepository.save(request);
    return request;
  }
}
