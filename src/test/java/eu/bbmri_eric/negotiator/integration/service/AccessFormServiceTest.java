package eu.bbmri_eric.negotiator.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.bbmri_eric.negotiator.database.model.AccessCriteria;
import eu.bbmri_eric.negotiator.database.model.AccessCriteriaSection;
import eu.bbmri_eric.negotiator.database.model.AccessCriteriaSectionLink;
import eu.bbmri_eric.negotiator.database.model.AccessCriteriaSet;
import eu.bbmri_eric.negotiator.database.model.DataSource;
import eu.bbmri_eric.negotiator.database.model.Organization;
import eu.bbmri_eric.negotiator.database.model.Request;
import eu.bbmri_eric.negotiator.database.model.Resource;
import eu.bbmri_eric.negotiator.database.repository.AccessCriteriaSetRepository;
import eu.bbmri_eric.negotiator.database.repository.DataSourceRepository;
import eu.bbmri_eric.negotiator.database.repository.OrganizationRepository;
import eu.bbmri_eric.negotiator.database.repository.RequestRepository;
import eu.bbmri_eric.negotiator.database.repository.ResourceRepository;
import eu.bbmri_eric.negotiator.dto.access_criteria.AccessCriteriaSetDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestCreateDTO;
import eu.bbmri_eric.negotiator.dto.request.RequestDTO;
import eu.bbmri_eric.negotiator.integration.api.v3.TestUtils;
import eu.bbmri_eric.negotiator.service.AccessCriteriaSetService;
import eu.bbmri_eric.negotiator.service.AccessFormService;
import eu.bbmri_eric.negotiator.service.RequestService;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
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

  @Autowired AccessCriteriaSetRepository accessCriteriaSetRepository;

  @Autowired DataSourceRepository dataSourceRepository;

  @Autowired OrganizationRepository organizationRepository;

  @Test
  void getAccessFormForRequest_nullId_throwsEntityNotFound() {
    assertThrows(
        IllegalArgumentException.class, () -> accessFormService.getAccessFormForRequest(null));
  }

  @Test
  void getAccessFormForRequest_1resource_identicalFormToResource() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    assertEquals(1, requestDTO.getResources().size());
    AccessCriteriaSetDTO resourceForm =
        accessCriteriaSetService.findByResourceId(
            requestDTO.getResources().iterator().next().getId());
    AccessCriteriaSetDTO requestForm =
        accessFormService.getAccessFormForRequest(requestDTO.getId());
    assertEquals(resourceForm, requestForm);
  }

  @Test
  @Transactional
  void getAccessFormForRequest_5resourceWithIdenticalForm_identicalForm() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Request request = requestRepository.findById(requestDTO.getId()).get();
    AccessCriteriaSet accessCriteriaSet = accessCriteriaSetRepository.findAll().get(0);
    request = addResourcesToRequest(accessCriteriaSet, request);
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
    SortedSet<AccessCriteriaSection> sections = new TreeSet<>();
    AccessCriteria accessCriteria =
        AccessCriteria.builder()
            .name("test")
            .label("test")
            .description("test")
            .type("text")
            .build();
    AccessCriteriaSectionLink accessCriteriaSectionLink =
        AccessCriteriaSectionLink.builder()
            .accessCriteria(accessCriteria)
            .ordering(0)
            .required(true)
            .build();
    sections.add(
        AccessCriteriaSection.builder()
            .id(100L)
            .name("section1")
            .label("test")
            .description("test")
            .accessCriteriaSectionLink(List.of(accessCriteriaSectionLink))
            .build());
    AccessCriteriaSet accessCriteriaSet =
        AccessCriteriaSet.builder().name("different_form").sections(sections).build();
    accessCriteriaSet = accessCriteriaSetRepository.save(accessCriteriaSet);
    resource.setAccessCriteriaSet(accessCriteriaSet);
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
  void getAccessFormForRequest_2resourcesWithSameSectionDifferentElements_combined() {
    RequestCreateDTO requestCreateDTO = TestUtils.createRequest(false);
    RequestDTO requestDTO = requestService.create(requestCreateDTO);
    Resource resource = resourceRepository.findAll().get(1);
    assertFalse(
        requestDTO.getResources().stream()
            .anyMatch(res -> res.getId().equals(resource.getSourceId())));
    AccessCriteria accessCriteria =
        AccessCriteria.builder()
            .name("test")
            .label("test")
            .description("test")
            .type("text")
            .build();
    AccessCriteriaSectionLink accessCriteriaSectionLink =
        AccessCriteriaSectionLink.builder()
            .accessCriteria(accessCriteria)
            .ordering(9)
            .required(true)
            .build();
    AccessCriteriaSet accessCriteriaSet = accessCriteriaSetRepository.findAll().get(0);
    accessCriteriaSet.setId(2L);
    accessCriteriaSet.setName("different_form");
    assertEquals(
        2, accessCriteriaSet.getSections().iterator().next().getAccessCriteriaSectionLink().size());
    accessCriteriaSet
        .getSections()
        .iterator()
        .next()
        .getAccessCriteriaSectionLink()
        .add(accessCriteriaSectionLink);
    accessCriteriaSet = accessCriteriaSetRepository.save(accessCriteriaSet);
    resource.setAccessCriteriaSet(accessCriteriaSet);
    resourceRepository.save(resource);
    Request request = requestRepository.findById(requestDTO.getId()).get();
    request.getResources().add(resource);
    request = requestRepository.save(request);
    assertEquals(2, request.getResources().size());
    assertEquals(
        3, accessFormService.getAccessFormForRequest(request.getId()).getSections().size());
    assertEquals(
        3, accessCriteriaSet.getSections().iterator().next().getAccessCriteriaSectionLink().size());
  }

  private Request addResourcesToRequest(AccessCriteriaSet accessCriteriaSet, Request request) {
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
                  .accessCriteriaSet(accessCriteriaSet)
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
